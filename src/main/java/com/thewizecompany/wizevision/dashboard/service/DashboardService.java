package com.thewizecompany.wizevision.dashboard.service;

import com.thewizecompany.wizevision.attendance.repository.AttendanceRepository;
import com.thewizecompany.wizevision.bidding.domain.BidStatus;
import com.thewizecompany.wizevision.bidding.repository.BidRepository;
import com.thewizecompany.wizevision.client.domain.Client;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.dashboard.dto.AdminDashboardResponse;
import com.thewizecompany.wizevision.dashboard.dto.FinanceDashboardResponse;
import com.thewizecompany.wizevision.dashboard.dto.HrDashboardResponse;
import com.thewizecompany.wizevision.dashboard.dto.PmDashboardResponse;
import com.thewizecompany.wizevision.employee.domain.EmploymentType;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.hr.domain.LeaveStatus;
import com.thewizecompany.wizevision.hr.repository.LeaveApplicationRepository;
import com.thewizecompany.wizevision.hr.repository.PayrollRunRepository;
import com.thewizecompany.wizevision.invoicing.domain.Invoice;
import com.thewizecompany.wizevision.invoicing.domain.InvoiceStatus;
import com.thewizecompany.wizevision.invoicing.domain.Payment;
import com.thewizecompany.wizevision.invoicing.repository.InvoiceRepository;
import com.thewizecompany.wizevision.invoicing.repository.PaymentRepository;
import com.thewizecompany.wizevision.marketing.domain.InquiryStatus;
import com.thewizecompany.wizevision.marketing.repository.ProjectInquiryRepository;
import com.thewizecompany.wizevision.projects.domain.ProjectStatus;
import com.thewizecompany.wizevision.projects.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final BidRepository bidRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final LeaveApplicationRepository
            leaveApplicationRepository;
    private final AttendanceRepository attendanceRepository;
    private final ProjectInquiryRepository inquiryRepository;
    private final ClientRepository clientRepository;
    private final PayrollRunRepository payrollRunRepository;

    // ─────────────────────────────────────────────────────────
    // ADMIN DASHBOARD
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);

        /*
         * Employee counts.
         */
        var allActive = employeeRepository.findAll()
                .stream()
                .filter(e -> !e.isDeleted() && e.isActive())
                .toList();

        long totalEmployees = allActive.size();

        long onLeaveToday = leaveApplicationRepository
                .findByStatusAndIsDeletedFalse(LeaveStatus.APPROVED)
                .stream()
                .filter(la ->
                        !la.getFromDate().isAfter(today)
                                && !la.getToDate().isBefore(today)
                )
                .count();

        long newJoinees = allActive.stream()
                .filter(e -> e.getJoiningDate() != null
                        && !e.getJoiningDate().isBefore(monthStart)
                )
                .count();

        /*
         * Project counts.
         */
        long activeProjects = projectRepository
                .countByStatusAndIsDeletedFalse(
                        ProjectStatus.ACTIVE
                );
        long planningProjects = projectRepository
                .countByStatusAndIsDeletedFalse(
                        ProjectStatus.PLANNING
                );
        long onHoldProjects = projectRepository
                .countByStatusAndIsDeletedFalse(
                        ProjectStatus.ON_HOLD
                );
        long deliveredThisMonth = projectRepository
                .findAll()
                .stream()
                .filter(p -> !p.isDeleted()
                        && p.getStatus() == ProjectStatus.DELIVERED
                        && p.getActualEndDate() != null
                        && !p.getActualEndDate().isBefore(monthStart)
                )
                .count();

        /*
         * Bid counts.
         */
        long pendingReview = bidRepository
                .countByStatusAndIsDeletedFalse(
                        BidStatus.SUBMITTED
                );
        long negotiating = bidRepository
                .countByStatusAndIsDeletedFalse(
                        BidStatus.NEGOTIATING
                );
        long acceptedThisMonth = bidRepository.findAll()
                .stream()
                .filter(b -> !b.isDeleted()
                        && b.getStatus() == BidStatus.ACCEPTED
                        && b.getDecidedAt() != null
                        && b.getDecidedAt()
                        .isAfter(monthStart.atStartOfDay()
                                .toInstant(java.time.ZoneOffset.UTC))
                )
                .count();
        long rejectedThisMonth = bidRepository.findAll()
                .stream()
                .filter(b -> !b.isDeleted()
                        && b.getStatus() == BidStatus.REJECTED
                        && b.getDecidedAt() != null
                        && b.getDecidedAt()
                        .isAfter(monthStart.atStartOfDay()
                                .toInstant(java.time.ZoneOffset.UTC))
                )
                .count();

        /*
         * Financial summary.
         */
        BigDecimal totalOutstanding =
                invoiceRepository.getTotalOutstanding();

        BigDecimal revenueThisMonth = invoiceRepository
                .findAll()
                .stream()
                .filter(i -> !i.isDeleted()
                        && i.getStatus() != InvoiceStatus.CANCELLED
                        && !i.getInvoiceDate().isBefore(monthStart)
                )
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueLastMonth = invoiceRepository
                .findAll()
                .stream()
                .filter(i -> !i.isDeleted()
                        && i.getStatus() != InvoiceStatus.CANCELLED
                        && !i.getInvoiceDate().isBefore(lastMonthStart)
                        && !i.getInvoiceDate().isAfter(lastMonthEnd)
                )
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var overdueInvoices = invoiceRepository
                .findOverdueInvoices(today);
        long overdueCount = overdueInvoices.size();
        BigDecimal overdueAmount = overdueInvoices.stream()
                .map(Invoice::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        /*
         * HR summary.
         */
        long pendingHrLeaves =
                leaveApplicationRepository
                        .findByStatusAndIsDeletedFalse(
                                LeaveStatus.PM_APPROVED
                        ).size()
                        + leaveApplicationRepository
                        .findByStatusAndIsDeletedFalse(
                                LeaveStatus.TL_APPROVED
                        ).size();

        return AdminDashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(totalEmployees)
                .employeesOnLeaveToday(onLeaveToday)
                .newJoineesThisMonth(newJoinees)
                .totalActiveProjects(activeProjects)
                .projectsInPlanning(planningProjects)
                .projectsOnHold(onHoldProjects)
                .projectsDeliveredThisMonth(deliveredThisMonth)
                .bidsSubmittedPendingReview(pendingReview)
                .bidsNegotiating(negotiating)
                .bidsAcceptedThisMonth(acceptedThisMonth)
                .bidsRejectedThisMonth(rejectedThisMonth)
                .totalOutstandingAmount(totalOutstanding)
                .revenueThisMonth(revenueThisMonth)
                .revenueLastMonth(revenueLastMonth)
                .overdueInvoicesCount(overdueCount)
                .overdueInvoicesAmount(overdueAmount)
                .leaveApplicationsPendingHr(pendingHrLeaves)
                .recentActivity(List.of())  //TODO: Add logs here
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // PM DASHBOARD
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PmDashboardResponse getPmDashboard(UUID pmId) {

        long activeProjects = projectRepository
                .findByPmIdAndStatusAndIsDeletedFalse(
                        pmId, ProjectStatus.ACTIVE
                ).size();

        long planningProjects = projectRepository
                .findByPmIdAndStatusAndIsDeletedFalse(
                        pmId, ProjectStatus.PLANNING
                ).size();

        long onHoldProjects = projectRepository
                .findByPmIdAndStatusAndIsDeletedFalse(
                        pmId, ProjectStatus.ON_HOLD
                ).size();

        long draftBids = bidRepository
                .findByCreatedByPmIdAndStatusAndIsDeletedFalse(
                        pmId, BidStatus.DRAFT
                ).size();

        long submittedBids = bidRepository
                .findByCreatedByPmIdAndStatusAndIsDeletedFalse(
                        pmId, BidStatus.SUBMITTED
                ).size();

        long inquiriesWaiting = inquiryRepository
                .findByForwardedToIdAndStatusAndIsDeletedFalse(
                        pmId, InquiryStatus.FORWARDED
                ).size();

        long leaveApprovals = leaveApplicationRepository
                .findByPmIdAndStatusAndIsDeletedFalse(
                        pmId, LeaveStatus.TL_APPROVED
                ).size();

        /*
         * Active projects summary for dashboard cards.
         */
        var activeProjectList = projectRepository
                .findByPmIdAndStatusAndIsDeletedFalse(
                        pmId, ProjectStatus.ACTIVE
                )
                .stream()
                .limit(5)
                .map(p -> {
                    String clientName = clientRepository
                            .findByIdAndIsDeletedFalse(
                                    p.getClientId()
                            )
                            .map(Client::getCompanyName)
                            .orElse(null);

                    return PmDashboardResponse
                            .ProjectSummaryItem.builder()
                            .projectNumber(p.getProjectNumber())
                            .projectName(p.getProjectName())
                            .clientName(clientName)
                            .status(p.getStatus().getDisplayName())
                            .phase(
                                    p.getCurrentPhase().getDisplayName()
                            )
                            .progressPercentage(
                                    p.getProgressPercentage()
                            )
                            .teamAssigned(p.isTeamAssigned())
                            .build();
                })
                .toList();

        /*
         * Pending inquiries summary.
         */
        var pendingInquiryList = inquiryRepository
                .findByForwardedToIdAndStatusAndIsDeletedFalse(
                        pmId, InquiryStatus.FORWARDED
                )
                .stream()
                .limit(5)
                .map(inq -> {
                    String clientName = inq.getClientId() != null
                            ? clientRepository
                              .findByIdAndIsDeletedFalse(
                                      inq.getClientId()
                              )
                              .map(Client::getCompanyName)
                              .orElse(null)
                            : null;

                    return PmDashboardResponse
                            .InquirySummaryItem.builder()
                            .inquiryNumber(inq.getInquiryNumber())
                            .projectName(inq.getProjectName())
                            .clientName(clientName)
                            .forwardedAt(
                                    inq.getForwardedAt() != null
                                            ? inq.getForwardedAt().toString()
                                            : null
                            )
                            .build();
                })
                .toList();

        return PmDashboardResponse.builder()
                .myActiveProjects(activeProjects)
                .myProjectsInPlanning(planningProjects)
                .myProjectsOnHold(onHoldProjects)
                .myDraftBids(draftBids)
                .mySubmittedBids(submittedBids)
                .inquiriesWaitingForBid(inquiriesWaiting)
                .leaveApprovalsNeeded(leaveApprovals)
                .activeProjects(activeProjectList)
                .pendingInquiries(pendingInquiryList)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // HR DASHBOARD
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public HrDashboardResponse getHrDashboard() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        var allActive = employeeRepository.findAll()
                .stream()
                .filter(e -> !e.isDeleted() && e.isActive())
                .toList();

        long fullTime = allActive.stream()
                .filter(e ->
                        e.getEmploymentType()
                                == EmploymentType.FULL_TIME
                )
                .count();
        long contract = allActive.stream()
                .filter(e ->
                        e.getEmploymentType()
                                == EmploymentType.CONTRACT
                )
                .count();
        long freelance = allActive.stream()
                .filter(e ->
                        e.getEmploymentType()
                                == EmploymentType.FREELANCE
                )
                .count();

        long onLeaveToday = leaveApplicationRepository
                .findByStatusAndIsDeletedFalse(LeaveStatus.APPROVED)
                .stream()
                .filter(la ->
                        !la.getFromDate().isAfter(today)
                                && !la.getToDate().isBefore(today)
                )
                .count();

        long newJoinees = allActive.stream()
                .filter(e -> e.getJoiningDate() != null
                        && !e.getJoiningDate().isBefore(monthStart))
                .count();

        long pendingLeaves =
                leaveApplicationRepository
                        .findByStatusAndIsDeletedFalse(
                                LeaveStatus.PENDING
                        ).size()
                        + leaveApplicationRepository
                        .findByStatusAndIsDeletedFalse(
                                LeaveStatus.TL_APPROVED
                        ).size()
                        + leaveApplicationRepository
                        .findByStatusAndIsDeletedFalse(
                                LeaveStatus.PM_APPROVED
                        ).size();

        /*
         * Check if payroll is processed this month.
         */
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();
        boolean payrollProcessed = payrollRunRepository
                .findByMonthAndYearAndIsDeletedFalse(
                        currentMonth, currentYear
                )
                .map(run ->
                        run.getStatus()
                                != com.thewizecompany.wizevision
                                .hr.domain.PayrollRun
                                .PayrollStatus.DRAFT
                )
                .orElse(false);

        /*
         * Headcount by department.
         */
        Map<String, Long> byDept = allActive.stream()
                .filter(e -> e.getDepartment() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment().getName(),
                        Collectors.counting()
                ));

        List<HrDashboardResponse.DepartmentHeadcountItem>
                deptList = byDept.entrySet().stream()
                .map(entry ->
                        HrDashboardResponse
                                .DepartmentHeadcountItem.builder()
                                .department(entry.getKey())
                                .count(entry.getValue())
                                .build()
                )
                .toList();

        return HrDashboardResponse.builder()
                .totalHeadcount(allActive.size())
                .fullTimeCount(fullTime)
                .contractCount(contract)
                .freelanceCount(freelance)
                .pendingLeaveApplications(pendingLeaves)
                .employeesOnLeaveToday(onLeaveToday)
                .newJoineesThisMonth(newJoinees)
                .currentMonthPayrollTotal(BigDecimal.ZERO)
                .payrollProcessedThisMonth(payrollProcessed)
                .headcountByDepartment(deptList)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // FINANCE DASHBOARD
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinanceDashboardResponse getFinanceDashboard() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        var allInvoices = invoiceRepository.findAll()
                .stream()
                .filter(i -> !i.isDeleted()
                        && i.getStatus() != InvoiceStatus.CANCELLED
                )
                .toList();

        BigDecimal totalInvoiced = allInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = allInvoices.stream()
                .map(Invoice::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = totalInvoiced
                .subtract(totalCollected);

        BigDecimal invoicedThisMonth = allInvoices.stream()
                .filter(i ->
                        !i.getInvoiceDate().isBefore(monthStart)
                )
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collectedThisMonth = paymentRepository
                .findAll()
                .stream()
                .filter(p -> !p.isDeleted()
                        && !p.getPaymentDate().isBefore(monthStart)
                )
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var overdueInvoices =
                invoiceRepository.findOverdueInvoices(today);
        long overdueCount = overdueInvoices.size();
        BigDecimal overdueAmount = overdueInvoices.stream()
                .map(Invoice::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long draftCount = allInvoices.stream()
                .filter(i ->
                        i.getStatus() == InvoiceStatus.DRAFT
                )
                .count();
        long sentCount = allInvoices.stream()
                .filter(i ->
                        i.getStatus() == InvoiceStatus.SENT
                )
                .count();
        long partialCount = allInvoices.stream()
                .filter(i ->
                        i.getStatus()
                                == InvoiceStatus.PARTIALLY_PAID
                )
                .count();

        /*
         * Invoice aging buckets.
         * Based on days since invoice date for unpaid invoices.
         */
        BigDecimal aging0to30 = BigDecimal.ZERO;
        BigDecimal aging31to60 = BigDecimal.ZERO;
        BigDecimal aging61to90 = BigDecimal.ZERO;
        BigDecimal agingOver90 = BigDecimal.ZERO;

        for (var invoice : allInvoices) {
            if (invoice.getOutstandingAmount()
                    .compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            long daysSince = today.toEpochDay()
                    - invoice.getInvoiceDate().toEpochDay();

            BigDecimal outstanding =
                    invoice.getOutstandingAmount();

            if (daysSince <= 30) {
                aging0to30 = aging0to30.add(outstanding);
            } else if (daysSince <= 60) {
                aging31to60 = aging31to60.add(outstanding);
            } else if (daysSince <= 90) {
                aging61to90 = aging61to90.add(outstanding);
            } else {
                agingOver90 = agingOver90.add(outstanding);
            }
        }

        /*
         * Top outstanding clients.
         */
        Map<UUID, BigDecimal> outstandingByClient =
                allInvoices.stream()
                        .filter(i -> i.getOutstandingAmount()
                                .compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.groupingBy(
                                Invoice::getClientId,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Invoice::getOutstandingAmount,
                                        BigDecimal::add
                                )
                        ));

        List<FinanceDashboardResponse.TopOutstandingClientItem>
                topClients = outstandingByClient.entrySet()
                .stream()
                .sorted((a, b) ->
                        b.getValue().compareTo(a.getValue())
                )
                .limit(5)
                .map(entry -> {
                    var client = clientRepository
                            .findByIdAndIsDeletedFalse(
                                    entry.getKey()
                            )
                            .orElse(null);

                    long overdueForClient = overdueInvoices
                            .stream()
                            .filter(i -> i.getClientId()
                                    .equals(entry.getKey()))
                            .count();

                    return FinanceDashboardResponse
                            .TopOutstandingClientItem.builder()
                            .clientName(client != null
                                    ? client.getCompanyName() : "Unknown")
                            .clientCode(client != null
                                    ? client.getCompanyCode() : null)
                            .outstandingAmount(entry.getValue())
                            .overdueInvoices(overdueForClient)
                            .build();
                })
                .toList();

        return FinanceDashboardResponse.builder()
                .totalInvoicedAllTime(totalInvoiced)
                .totalCollectedAllTime(totalCollected)
                .totalOutstanding(totalOutstanding)
                .invoicedThisMonth(invoicedThisMonth)
                .collectedThisMonth(collectedThisMonth)
                .overdueCount(overdueCount)
                .overdueAmount(overdueAmount)
                .draftInvoicesCount(draftCount)
                .sentInvoicesCount(sentCount)
                .partiallyPaidCount(partialCount)
                .aging0to30(aging0to30)
                .aging31to60(aging31to60)
                .aging61to90(aging61to90)
                .agingOver90(agingOver90)
                .topOutstandingClients(topClients)
                .build();
    }
}