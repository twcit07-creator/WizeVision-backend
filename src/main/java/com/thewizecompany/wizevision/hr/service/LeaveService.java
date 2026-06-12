package com.thewizecompany.wizevision.hr.service;

import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.domain.Role;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.hr.domain.LeaveApplication;
import com.thewizecompany.wizevision.hr.domain.LeaveBalance;
import com.thewizecompany.wizevision.hr.domain.LeaveStatus;
import com.thewizecompany.wizevision.hr.dto.ApplyLeaveRequest;
import com.thewizecompany.wizevision.hr.dto.LeaveActionRequest;
import com.thewizecompany.wizevision.hr.dto.LeaveApplicationResponse;
import com.thewizecompany.wizevision.hr.dto.LeaveBalanceResponse;
import com.thewizecompany.wizevision.hr.repository.LeaveApplicationRepository;
import com.thewizecompany.wizevision.hr.repository.LeaveBalanceRepository;
import com.thewizecompany.wizevision.hr.repository.LeaveTypeRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveApplicationRepository applicationRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;

    // ─────────────────────────────────────────────────────────
    // APPLY FOR LEAVE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public LeaveApplicationResponse applyForLeave(
            UUID employeeId,
            ApplyLeaveRequest request) {

        var employee = employeeRepository
                .findByIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee", employeeId.toString()
                        )
                );

        var leaveType = leaveTypeRepository
                .findByIdAndIsDeletedFalse(
                        request.getLeaveTypeId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "LeaveType",
                                request.getLeaveTypeId().toString()
                        )
                );

        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new BusinessException(
                    "To date cannot be before from date",
                    "INVALID_DATE_RANGE"
            );
        }

        /*
         * Check for overlapping applications.
         */
        long overlapping = applicationRepository
                .countOverlappingLeaves(
                        employeeId,
                        request.getFromDate(),
                        request.getToDate()
                );

        if (overlapping > 0) {
            throw new BusinessException(
                    "You already have a leave application " +
                            "for overlapping dates",
                    "LEAVE_OVERLAP"
            );
        }

        /*
         * Calculate business days.
         * Excludes weekends.
         */
        BigDecimal totalDays = calculateBusinessDays(
                request.getFromDate(),
                request.getToDate()
        );

        /*
         * Check leave balance for paid leave types.
         */
        if (leaveType.isPaid()
                && !leaveType.getCode().equals("UNPAID")) {
            int year = request.getFromDate().getYear();

            /*
             * Initialize balance if not exists.
             */
            ensureBalanceExists(
                    employeeId,
                    leaveType.getId(),
                    year
            );

            var balance = balanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYearAndIsDeletedFalse(
                            employeeId,
                            leaveType.getId(),
                            year
                    )
                    .orElseThrow();

            if (balance.getRemainingDays().compareTo(totalDays) < 0) {
                throw new BusinessException(
                        "Insufficient leave balance. " +
                                "Available: " + balance.getRemainingDays()
                                + " days. Requested: " + totalDays + " days.",
                        "INSUFFICIENT_LEAVE_BALANCE"
                );
            }

            /*
             * Reserve the days as pending.
             */
            balance.setPendingDays(
                    balance.getPendingDays().add(totalDays)
            );
            balanceRepository.save(balance);
        }

        /*
         * Determine approval chain.
         * Find TL and PM from the employee's reporting structure.
         */
        UUID tlId = findTeamLead(employeeId);
        UUID pmId = findProjectManager(employeeId);

        /*
         * Determine initial status based on approval chain.
         * If no TL → skip TL step.
         */
        LeaveStatus initialStatus;
        UUID routedTlId = null;
        UUID routedPmId = null;

        if (tlId != null) {
            initialStatus = LeaveStatus.PENDING;
            routedTlId = tlId;
            routedPmId = pmId;
        } else if (pmId != null) {
            initialStatus = LeaveStatus.PENDING;
            routedPmId = pmId;
        } else {
            /*
             * No TL or PM → goes directly to HR.
             */
            initialStatus = LeaveStatus.PENDING;
        }

        String applicationNumber = generateApplicationNumber();

        LeaveApplication application = LeaveApplication.builder()
                .applicationNumber(applicationNumber)
                .employeeId(employeeId)
                .leaveTypeId(leaveType.getId())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .totalDays(totalDays)
                .reason(request.getReason())
                .supportingDocumentUrl(
                        request.getSupportingDocumentUrl()
                )
                .status(initialStatus)
                .tlId(routedTlId)
                .pmId(routedPmId)
                .build();

        LeaveApplication saved =
                applicationRepository.save(application);

        log.info(
                "Leave application created: {} for employee: {}",
                saved.getApplicationNumber(),
                employeeId
        );

        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // TL ACTION
    // ─────────────────────────────────────────────────────────

    @Transactional
    public LeaveApplicationResponse tlAction(
            UUID applicationId,
            UUID tlId,
            LeaveActionRequest request) {

        LeaveApplication application =
                findApplication(applicationId);

        if (application.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException(
                    "Application is not pending TL approval",
                    "INVALID_STATUS"
            );
        }

        if (!tlId.equals(application.getTlId())) {
            throw new BusinessException(
                    "You are not the assigned Team Lead " +
                            "for this application",
                    "NOT_AUTHORIZED"
            );
        }

        application.setTlId(tlId);
        application.setTlActionAt(Instant.now());
        application.setTlRemarks(request.getRemarks());

        if (request.getAction()
                == LeaveActionRequest.LeaveAction.APPROVE) {

            if (application.getPmId() != null) {
                application.setStatus(
                        LeaveStatus.TL_APPROVED
                );
            } else {
                /*
                 * No PM → TL approval is final.
                 * HR will confirm.
                 */
                application.setStatus(
                        LeaveStatus.TL_APPROVED
                );
            }
        } else {
            application.setStatus(LeaveStatus.REJECTED);
            revertPendingBalance(application);
        }

        LeaveApplication saved =
                applicationRepository.save(application);

        log.info(
                "TL {} {} leave application: {}",
                tlId,
                request.getAction(),
                application.getApplicationNumber()
        );

        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // PM ACTION
    // ─────────────────────────────────────────────────────────

    @Transactional
    public LeaveApplicationResponse pmAction(
            UUID applicationId,
            UUID pmId,
            LeaveActionRequest request) {

        LeaveApplication application =
                findApplication(applicationId);

        if (application.getStatus() != LeaveStatus.PENDING
                && application.getStatus()
                != LeaveStatus.TL_APPROVED) {
            throw new BusinessException(
                    "Application is not pending PM approval",
                    "INVALID_STATUS"
            );
        }

        if (!pmId.equals(application.getPmId())) {
            throw new BusinessException(
                    "You are not the assigned Project Manager " +
                            "for this application",
                    "NOT_AUTHORIZED"
            );
        }

        application.setPmId(pmId);
        application.setPmActionAt(Instant.now());
        application.setPmRemarks(request.getRemarks());

        if (request.getAction()
                == LeaveActionRequest.LeaveAction.APPROVE) {
            application.setStatus(LeaveStatus.PM_APPROVED);
        } else {
            application.setStatus(LeaveStatus.REJECTED);
            revertPendingBalance(application);
        }

        LeaveApplication saved =
                applicationRepository.save(application);

        log.info(
                "PM {} {} leave application: {}",
                pmId,
                request.getAction(),
                application.getApplicationNumber()
        );

        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // HR FINAL APPROVAL
    // ─────────────────────────────────────────────────────────

    @Transactional
    public LeaveApplicationResponse hrAction(
            UUID applicationId,
            UUID hrId,
            LeaveActionRequest request) {

        LeaveApplication application =
                findApplication(applicationId);

        if (application.getStatus() != LeaveStatus.PM_APPROVED
                && application.getStatus()
                != LeaveStatus.TL_APPROVED
                && application.getStatus()
                != LeaveStatus.PENDING) {
            throw new BusinessException(
                    "Application cannot be acted on " +
                            "at this stage",
                    "INVALID_STATUS"
            );
        }

        application.setHrId(hrId);
        application.setHrActionAt(Instant.now());
        application.setHrRemarks(request.getRemarks());

        if (request.getAction()
                == LeaveActionRequest.LeaveAction.APPROVE) {

            application.setStatus(LeaveStatus.APPROVED);

            /*
             * Convert pending to used in leave balance.
             */
            var leaveType = leaveTypeRepository
                    .findByIdAndIsDeletedFalse(
                            application.getLeaveTypeId()
                    )
                    .orElse(null);

            if (leaveType != null
                    && leaveType.isPaid()
                    && !leaveType.getCode().equals("UNPAID")) {

                int year = application.getFromDate().getYear();
                balanceRepository
                        .findByEmployeeIdAndLeaveTypeIdAndYearAndIsDeletedFalse(
                                application.getEmployeeId(),
                                application.getLeaveTypeId(),
                                year
                        )
                        .ifPresent(balance -> {
                            balance.setPendingDays(balance.getPendingDays().subtract(application.getTotalDays()).max(BigDecimal.ZERO));
                            balance.setUsedDays(
                                    balance.getUsedDays().add(
                                            application.getTotalDays())
                            );
                            balanceRepository.save(balance);
                        });
            }

        } else {
            application.setStatus(LeaveStatus.REJECTED);
            revertPendingBalance(application);
        }

        LeaveApplication saved =
                applicationRepository.save(application);

        log.info(
                "HR {} {} leave application: {}",
                hrId,
                request.getAction(),
                application.getApplicationNumber()
        );

        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // CANCEL (Employee)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public LeaveApplicationResponse cancelLeave(
            UUID applicationId,
            UUID employeeId) {

        LeaveApplication application =
                findApplication(applicationId);

        if (!application.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(
                    "You can only cancel your own leave",
                    "NOT_AUTHORIZED"
            );
        }

        if (application.getStatus() == LeaveStatus.APPROVED
                && application.getFromDate()
                .isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "Cannot cancel leave that has already started",
                    "LEAVE_ALREADY_STARTED"
            );
        }

        application.setStatus(LeaveStatus.CANCELLED);
        revertPendingBalance(application);

        return mapToResponse(
                applicationRepository.save(application)
        );
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<LeaveApplicationResponse> getMyLeaves(
            UUID employeeId,
            Pageable pageable) {

        return PageResponse.from(
                applicationRepository
                        .findByEmployeeIdAndIsDeletedFalse(
                                employeeId, pageable
                        )
                        .map(this::mapToResponse)
        );
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getMyBalances(
            UUID employeeId) {

        int year = Year.now().getValue();
        var leaveTypes =
                leaveTypeRepository
                        .findByIsActiveTrueAndIsDeletedFalse();

        List<LeaveBalanceResponse> responses = new ArrayList<>();

        for (var leaveType : leaveTypes) {
            ensureBalanceExists(
                    employeeId, leaveType.getId(), year
            );

            balanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYearAndIsDeletedFalse(
                            employeeId, leaveType.getId(), year
                    ).ifPresent(balance -> responses.add(
                            LeaveBalanceResponse.builder()
                                    .leaveTypeId(leaveType.getId())
                                    .leaveTypeName(leaveType.getName())
                                    .leaveTypeCode(leaveType.getCode())
                                    .isPaid(leaveType.isPaid())
                                    .year(year)
                                    .totalDays(balance.getTotalDays())
                                    .usedDays(balance.getUsedDays())
                                    .pendingDays(balance.getPendingDays())
                                    .carriedForwardDays(
                                            balance.getCarriedForwardDays()
                                    )
                                    .remainingDays(
                                            balance.getRemainingDays()
                                    )
                                    .build()
                    ));

        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getPendingForTl(
            UUID tlId) {
        return applicationRepository
                .findByTlIdAndStatusAndIsDeletedFalse(
                        tlId, LeaveStatus.PENDING
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getPendingForPm(
            UUID pmId) {
        return applicationRepository
                .findByPmIdAndStatusAndIsDeletedFalse(
                        pmId, LeaveStatus.TL_APPROVED
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getPendingForHr() {
        List<LeaveApplication> pending = new ArrayList<>();
        pending.addAll(
                applicationRepository
                        .findByStatusAndIsDeletedFalse(
                                LeaveStatus.PM_APPROVED
                        )
        );
        pending.addAll(
                applicationRepository
                        .findByStatusAndIsDeletedFalse(
                                LeaveStatus.TL_APPROVED
                        )
        );
        return pending.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private LeaveApplication findApplication(UUID id) {
        return applicationRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "LeaveApplication", id.toString()
                        )
                );
    }

    private BigDecimal calculateBusinessDays(
            LocalDate from,
            LocalDate to) {

        BigDecimal count = BigDecimal.ZERO;
        LocalDate current = from;

        while (!current.isAfter(to)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY
                    && current.getDayOfWeek()
                    != DayOfWeek.SUNDAY) {
                count = count.add(BigDecimal.ONE);
            }
            current = current.plusDays(1);
        }

        return count;
    }

    private void ensureBalanceExists(
            UUID employeeId,
            UUID leaveTypeId,
            int year) {

        boolean exists = balanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYearAndIsDeletedFalse(
                        employeeId, leaveTypeId, year
                )
                .isPresent();

        if (!exists) {
            var leaveType = leaveTypeRepository
                    .findByIdAndIsDeletedFalse(leaveTypeId)
                    .orElse(null);

            if (leaveType != null) {
                LeaveBalance balance = LeaveBalance.builder()
                        .employeeId(employeeId)
                        .leaveTypeId(leaveTypeId)
                        .year(year)
                        .totalDays(
                                 BigDecimal.valueOf(leaveType
                                        .getDefaultDaysPerYear())
                        )
                        .usedDays(BigDecimal.ZERO)
                        .pendingDays(BigDecimal.ZERO)
                        .carriedForwardDays(BigDecimal.ZERO)
                        .build();

                balanceRepository.save(balance);
            }
        }
    }

    private void revertPendingBalance(
            LeaveApplication application) {

        var leaveType = leaveTypeRepository
                .findByIdAndIsDeletedFalse(
                        application.getLeaveTypeId()
                )
                .orElse(null);

        if (leaveType != null && leaveType.isPaid()) {
            int year = application.getFromDate().getYear();
            balanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYearAndIsDeletedFalse(
                            application.getEmployeeId(),
                            application.getLeaveTypeId(),
                            year
                    )
                    .ifPresent(balance -> {
                        balance.setPendingDays(
                                balance.getPendingDays().subtract(
                                        application.getTotalDays()).max(BigDecimal.ZERO)
                        );
                        balanceRepository.save(balance);
                    });
        }
    }

    private UUID findTeamLead(UUID employeeId) {
        /*
         * The reporting manager of the employee.
         * If reporting manager has TEAM_LEAD or similar role.
         * For now we use reportingManagerId directly.
         */
        return employeeRepository
                .findByIdAndIsDeletedFalse(employeeId).filter(e -> e.getReportingManager() != null).map(e -> e.getReportingManager().getId()).orElse(null);
    }

    private UUID findProjectManager(UUID employeeId) {
        /*
         * Walk up the reporting chain to find a PM.
         * Simplified: check if reporting manager is PM.
         */
        return employeeRepository
                .findByIdAndIsDeletedFalse(employeeId)
                .map(e -> {
                    if (e.getReportingManager() == null)
                        return null;
                    var manager = e.getReportingManager();
                    if (manager.getRole() == Role.PROJECT_MANAGER) {
                        return manager.getId();
                    }
                    return null;
                })
                .orElse(null);
    }

    private String generateApplicationNumber() {
        int year = Year.now().getValue();
        long count = applicationRepository.countByIsDeletedFalse();
        return "LA-" + year + "-"
                + String.format("%03d", count + 1);
    }

    private LeaveApplicationResponse mapToResponse(
            LeaveApplication app) {

        String employeeName = employeeRepository
                .findByIdAndIsDeletedFalse(app.getEmployeeId())
                .map(Employee::getFullName)
                .orElse(null);

        String employeeCode = employeeRepository
                .findByIdAndIsDeletedFalse(app.getEmployeeId())
                .map(Employee::getEmployeeCode)
                .orElse(null);

        String department = employeeRepository
                .findByIdAndIsDeletedFalse(app.getEmployeeId()).filter(e -> e.getDepartment() != null).map(e -> e.getDepartment().getName()).orElse(null);

        var leaveType = leaveTypeRepository
                .findByIdAndIsDeletedFalse(app.getLeaveTypeId())
                .orElse(null);

        String tlName = app.getTlId() != null
                ? employeeRepository
                  .findByIdAndIsDeletedFalse(app.getTlId())
                  .map(Employee::getFullName)
                  .orElse(null)
                : null;

        String pmName = app.getPmId() != null
                ? employeeRepository
                  .findByIdAndIsDeletedFalse(app.getPmId())
                  .map(Employee::getFullName)
                  .orElse(null)
                : null;

        String hrName = app.getHrId() != null
                ? employeeRepository
                  .findByIdAndIsDeletedFalse(app.getHrId())
                  .map(Employee::getFullName)
                  .orElse(null)
                : null;

        return LeaveApplicationResponse.builder()
                .id(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .employeeId(app.getEmployeeId())
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .department(department)
                .leaveTypeId(app.getLeaveTypeId())
                .leaveTypeName(
                        leaveType != null
                                ? leaveType.getName() : null
                )
                .leaveTypeCode(
                        leaveType != null
                                ? leaveType.getCode() : null
                )
                .isPaid(
                        leaveType != null && leaveType.isPaid()
                )
                .fromDate(app.getFromDate())
                .toDate(app.getToDate())
                .totalDays(app.getTotalDays())
                .reason(app.getReason())
                .supportingDocumentUrl(
                        app.getSupportingDocumentUrl()
                )
                .status(app.getStatus())
                .statusDisplay(
                        app.getStatus().getDisplayName()
                )
                .tlId(app.getTlId())
                .tlName(tlName)
                .tlActionAt(app.getTlActionAt())
                .tlRemarks(app.getTlRemarks())
                .pmId(app.getPmId())
                .pmName(pmName)
                .pmActionAt(app.getPmActionAt())
                .pmRemarks(app.getPmRemarks())
                .hrId(app.getHrId())
                .hrName(hrName)
                .hrActionAt(app.getHrActionAt())
                .hrRemarks(app.getHrRemarks())
                .createdAt(app.getCreatedAt())
                .build();
    }
}