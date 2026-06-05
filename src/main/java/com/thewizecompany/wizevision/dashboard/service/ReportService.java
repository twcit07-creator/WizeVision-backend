package com.thewizecompany.wizevision.dashboard.service;

import com.thewizecompany.wizevision.attendance.domain.AttendanceStatus;
import com.thewizecompany.wizevision.attendance.repository.AttendanceRepository;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.dashboard.dto.AttendanceReportResponse;
import com.thewizecompany.wizevision.dashboard.dto.FinancialReportResponse;
import com.thewizecompany.wizevision.dashboard.dto.LeaveReportResponse;
import com.thewizecompany.wizevision.dashboard.dto.ProjectReportResponse;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.hr.domain.LeaveStatus;
import com.thewizecompany.wizevision.hr.repository.LeaveApplicationRepository;
import com.thewizecompany.wizevision.hr.repository.LeaveBalanceRepository;
import com.thewizecompany.wizevision.hr.repository.LeaveTypeRepository;
import com.thewizecompany.wizevision.invoicing.domain.InvoiceStatus;
import com.thewizecompany.wizevision.invoicing.repository.InvoiceRepository;
import com.thewizecompany.wizevision.projects.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final LeaveApplicationRepository
            leaveApplicationRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    // ─────────────────────────────────────────────────────────
    // ATTENDANCE REPORT
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AttendanceReportResponse getAttendanceReport(
            LocalDate fromDate,
            LocalDate toDate,
            UUID departmentId) {

        int workingDays = countWorkingDays(fromDate, toDate);

        var employees = employeeRepository.findAll()
                .stream()
                .filter(e -> !e.isDeleted() && e.isActive())
                .filter(e -> departmentId == null
                        || (e.getDepartment() != null
                        && e.getDepartment().getId()
                        .equals(departmentId)))
                .toList();

        List<AttendanceReportResponse.EmployeeAttendanceSummary>
                summaries = new ArrayList<>();

        for (var employee : employees) {
            var records = attendanceRepository
                    .findByEmployeeIdAndAttendanceDateBetweenAndIsDeletedFalse(
                            employee.getId(), fromDate, toDate
                    );

            int presentDays = (int) records.stream()
                    .filter(r ->
                            r.getStatus() == AttendanceStatus.PRESENT
                                    || r.getStatus() == AttendanceStatus.LATE
                    )
                    .count();

            int lateDays = (int) records.stream()
                    .filter(r ->
                            r.getStatus() == AttendanceStatus.LATE
                    )
                    .count();

            int leaveDays = (int) records.stream()
                    .filter(r ->
                            r.getStatus()
                                    == AttendanceStatus.ON_LEAVE
                    )
                    .count();

            int halfDays = (int) records.stream()
                    .filter(r ->
                            r.getStatus()
                                    == AttendanceStatus.HALF_DAY
                    )
                    .count();

            int absentDays = workingDays
                    - presentDays - leaveDays - halfDays;

            double totalWorkMinutes = records.stream()
                    .mapToInt(r -> r.getTotalWorkMinutes() != null
                            ? r.getTotalWorkMinutes() : 0)
                    .sum();

            double totalIdleMinutes = records.stream()
                    .mapToInt(r -> r.getTotalIdleMinutes() != null
                            ? r.getTotalIdleMinutes() : 0)
                    .sum();

            double totalOvertimeMinutes = records.stream()
                    .mapToInt(r -> r.getOvertimeMinutes() != null
                            ? r.getOvertimeMinutes() : 0)
                    .sum();

            double attendancePct = workingDays > 0
                    ? (double) presentDays / workingDays * 100
                    : 0;

            summaries.add(
                    AttendanceReportResponse
                            .EmployeeAttendanceSummary.builder()
                            .employeeId(employee.getId())
                            .employeeCode(employee.getEmployeeCode())
                            .employeeName(employee.getFullName())
                            .department(
                                    employee.getDepartment() != null
                                            ? employee.getDepartment()
                                              .getName()
                                            : "N/A"
                            )
                            .presentDays(presentDays)
                            .absentDays(Math.max(0, absentDays))
                            .lateDays(lateDays)
                            .leaveDays(leaveDays)
                            .halfDays(halfDays)
                            .totalWorkHours(
                                    totalWorkMinutes / 60.0
                            )
                            .totalIdleHours(
                                    totalIdleMinutes / 60.0
                            )
                            .totalOvertimeHours(
                                    totalOvertimeMinutes / 60.0
                            )
                            .attendancePercentage(attendancePct)
                            .build()
            );
        }

        return AttendanceReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalWorkingDays(workingDays)
                .employees(summaries)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // PROJECT REPORT
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProjectReportResponse getProjectReport(
            String status,
            UUID clientId,
            LocalDate fromDate,
            LocalDate toDate) {

        var projects = projectRepository.findAll()
                .stream()
                .filter(p -> !p.isDeleted())
                .filter(p -> status == null
                        || p.getStatus().name().equals(status))
                .filter(p -> clientId == null
                        || p.getClientId().equals(clientId))
                .filter(p -> fromDate == null
                        || (p.getActualStartDate() != null
                        && !p.getActualStartDate()
                        .isBefore(fromDate)))
                .toList();

        BigDecimal totalContractValue = projects.stream()
                .map(p -> p.getTotalContractValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBilled = projects.stream()
                .map(p -> p.getTotalInvoiced())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = projects.stream()
                .map(p -> p.getTotalPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeCount = projects.stream()
                .filter(p -> p.getStatus()
                        == com.thewizecompany.wizevision
                        .projects.domain.ProjectStatus.ACTIVE)
                .count();

        long completedCount = projects.stream()
                .filter(p -> p.getStatus()
                        == com.thewizecompany.wizevision
                        .projects.domain.ProjectStatus.COMPLETED)
                .count();

        long cancelledCount = projects.stream()
                .filter(p -> p.getStatus()
                        == com.thewizecompany.wizevision
                        .projects.domain.ProjectStatus.CANCELLED)
                .count();

        List<ProjectReportResponse.ProjectReportItem> items =
                projects.stream().map(p -> {
                    String clientName = clientRepository
                            .findByIdAndIsDeletedFalse(
                                    p.getClientId()
                            )
                            .map(c -> c.getCompanyName())
                            .orElse(null);

                    String pmName = employeeRepository
                            .findByIdAndIsDeletedFalse(p.getPmId())
                            .map(e -> e.getFullName())
                            .orElse(null);

                    long daysRunning = p.getActualStartDate() != null
                            ? ChronoUnit.DAYS.between(
                            p.getActualStartDate(),
                            p.getActualEndDate() != null
                            ? p.getActualEndDate()
                            : LocalDate.now()
                    )
                            : 0;

                    return ProjectReportResponse
                            .ProjectReportItem.builder()
                            .projectNumber(p.getProjectNumber())
                            .projectName(p.getProjectName())
                            .clientName(clientName)
                            .pmName(pmName)
                            .status(p.getStatus().getDisplayName())
                            .currentPhase(
                                    p.getCurrentPhase().getDisplayName()
                            )
                            .progressPercentage(
                                    p.getProgressPercentage()
                            )
                            .startDate(p.getActualStartDate())
                            .endDate(p.getActualEndDate())
                            .contractAmount(p.getContractAmount())
                            .changeOrdersTotal(
                                    p.getChangeOrdersTotal()
                            )
                            .totalInvoiced(p.getTotalInvoiced())
                            .totalPaid(p.getTotalPaid())
                            .outstanding(p.getOutstandingAmount())
                            .daysRunning(daysRunning)
                            .build();
                }).toList();

        return ProjectReportResponse.builder()
                .totalProjects((long) projects.size())
                .activeProjects(activeCount)
                .completedProjects(completedCount)
                .cancelledProjects(cancelledCount)
                .totalContractValue(totalContractValue)
                .totalBilled(totalBilled)
                .totalCollected(totalCollected)
                .projects(items)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // FINANCIAL REPORT
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinancialReportResponse getFinancialReport(
            int year) {

        String[] monthNames = {
                "January","February","March","April",
                "May","June","July","August",
                "September","October","November","December"
        };

        List<FinancialReportResponse.MonthlyRevenuItem>
                monthly = new ArrayList<>();

        BigDecimal yearTotal = BigDecimal.ZERO;
        BigDecimal yearCollected = BigDecimal.ZERO;

        for (int m = 1; m <= 12; m++) {
            LocalDate start = LocalDate.of(year, m, 1);
            LocalDate end = start.withDayOfMonth(
                    start.lengthOfMonth()
            );

            int monthFinal = m;
            var monthInvoices = invoiceRepository.findAll()
                    .stream()
                    .filter(i -> !i.isDeleted()
                            && i.getStatus()
                            != InvoiceStatus.CANCELLED
                            && i.getInvoiceDate().getYear() == year
                            && i.getInvoiceDate().getMonthValue()
                            == monthFinal
                    )
                    .toList();

            BigDecimal monthTotal = monthInvoices.stream()
                    .map(i -> i.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthCollected = monthInvoices.stream()
                    .map(i -> i.getAmountPaid())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthOutstanding =
                    monthTotal.subtract(monthCollected);

            yearTotal = yearTotal.add(monthTotal);
            yearCollected = yearCollected.add(monthCollected);

            monthly.add(
                    FinancialReportResponse
                            .MonthlyRevenuItem.builder()
                            .month(monthNames[m - 1])
                            .monthNumber(m)
                            .invoiced(monthTotal)
                            .collected(monthCollected)
                            .outstanding(monthOutstanding)
                            .invoiceCount((long) monthInvoices.size())
                            .build()
            );
        }

        /*
         * Revenue by client.
         */
        var allInvoices = invoiceRepository.findAll()
                .stream()
                .filter(i -> !i.isDeleted()
                        && i.getStatus() != InvoiceStatus.CANCELLED
                        && i.getInvoiceDate().getYear() == year
                )
                .toList();

        var byClientMap = allInvoices.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        i -> i.getClientId()
                ));

        List<FinancialReportResponse.ClientRevenueItem>
                byClient = byClientMap.entrySet().stream()
                .map(entry -> {
                    var client = clientRepository
                            .findByIdAndIsDeletedFalse(
                                    entry.getKey()
                            )
                            .orElse(null);

                    BigDecimal billed = entry.getValue()
                            .stream()
                            .map(i -> i.getTotalAmount())
                            .reduce(BigDecimal.ZERO,
                                    BigDecimal::add);

                    BigDecimal collected = entry.getValue()
                            .stream()
                            .map(i -> i.getAmountPaid())
                            .reduce(BigDecimal.ZERO,
                                    BigDecimal::add);

                    long projCount = projectRepository
                            .findAll().stream()
                            .filter(p -> !p.isDeleted()
                                    && p.getClientId()
                                    .equals(entry.getKey()))
                            .count();

                    return FinancialReportResponse
                            .ClientRevenueItem.builder()
                            .clientName(client != null
                                    ? client.getCompanyName()
                                    : "Unknown")
                            .clientCode(client != null
                                    ? client.getCompanyCode() : null)
                            .projectCount(projCount)
                            .totalBilled(billed)
                            .totalCollected(collected)
                            .outstanding(
                                    billed.subtract(collected)
                            )
                            .build();
                })
                .sorted((a, b) ->
                        b.getTotalBilled()
                                .compareTo(a.getTotalBilled())
                )
                .toList();

        return FinancialReportResponse.builder()
                .year(year)
                .totalRevenue(yearTotal)
                .totalCollected(yearCollected)
                .totalOutstanding(
                        yearTotal.subtract(yearCollected)
                )
                .monthlyBreakdown(monthly)
                .byClient(byClient)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // LEAVE REPORT
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LeaveReportResponse getLeaveReport(
            int year,
            UUID departmentId) {

        var leaveTypes =
                leaveTypeRepository
                        .findByIsActiveTrueAndIsDeletedFalse();

        var employees = employeeRepository.findAll()
                .stream()
                .filter(e -> !e.isDeleted() && e.isActive())
                .filter(e -> departmentId == null
                        || (e.getDepartment() != null
                        && e.getDepartment().getId()
                        .equals(departmentId)))
                .toList();

        var allApplications = leaveApplicationRepository
                .findAll()
                .stream()
                .filter(a -> !a.isDeleted()
                        && a.getFromDate().getYear() == year
                )
                .toList();

        long totalApps = allApplications.size();
        long approvedApps = allApplications.stream()
                .filter(a ->
                        a.getStatus() == LeaveStatus.APPROVED
                )
                .count();
        long rejectedApps = allApplications.stream()
                .filter(a ->
                        a.getStatus() == LeaveStatus.REJECTED
                )
                .count();
        long pendingApps = allApplications.stream()
                .filter(a ->
                        a.getStatus() == LeaveStatus.PENDING
                                || a.getStatus() == LeaveStatus.TL_APPROVED
                                || a.getStatus() == LeaveStatus.PM_APPROVED
                )
                .count();

        /*
         * Per employee leave summary.
         */
        List<LeaveReportResponse.EmployeeLeaveItem>
                empItems = new ArrayList<>();

        for (var emp : employees) {
            var empApps = allApplications.stream()
                    .filter(a ->
                            a.getEmployeeId().equals(emp.getId())
                                    && a.getStatus() == LeaveStatus.APPROVED
                    )
                    .toList();

            double annual = 0, sick = 0,
                    casual = 0, unpaid = 0, total = 0;

            for (var app : empApps) {
                var lt = leaveTypes.stream()
                        .filter(t ->
                                t.getId().equals(app.getLeaveTypeId())
                        )
                        .findFirst()
                        .orElse(null);

                if (lt == null) continue;

                double days = app.getTotalDays()
                        .doubleValue();
                total += days;

                switch (lt.getCode()) {
                    case "ANNUAL" -> annual += days;
                    case "SICK"   -> sick   += days;
                    case "CASUAL" -> casual += days;
                    case "UNPAID" -> unpaid += days;
                }
            }

            empItems.add(
                    LeaveReportResponse.EmployeeLeaveItem
                            .builder()
                            .employeeCode(emp.getEmployeeCode())
                            .employeeName(emp.getFullName())
                            .department(emp.getDepartment() != null
                                    ? emp.getDepartment().getName()
                                    : "N/A")
                            .annualUsed(annual)
                            .sickUsed(sick)
                            .casualUsed(casual)
                            .unpaidUsed(unpaid)
                            .totalUsed(total)
                            .build()
            );
        }

        /*
         * Per leave type summary.
         */
        List<LeaveReportResponse.LeaveTypeItem>
                typeItems = leaveTypes.stream()
                .map(lt -> {
                    var ltApps = allApplications.stream()
                            .filter(a ->
                                    a.getLeaveTypeId()
                                            .equals(lt.getId())
                                            && a.getStatus()
                                            == LeaveStatus.APPROVED
                            )
                            .toList();

                    double totalDays = ltApps.stream()
                            .mapToDouble(a ->
                                    a.getTotalDays().doubleValue()
                            )
                            .sum();

                    return LeaveReportResponse.LeaveTypeItem
                            .builder()
                            .leaveType(lt.getName())
                            .totalDaysTaken(totalDays)
                            .applicationCount(
                                    (long) ltApps.size()
                            )
                            .build();
                })
                .toList();

        return LeaveReportResponse.builder()
                .year(year)
                .totalApplications(totalApps)
                .approvedApplications(approvedApps)
                .rejectedApplications(rejectedApps)
                .pendingApplications(pendingApps)
                .byEmployee(empItems)
                .byLeaveType(typeItems)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private int countWorkingDays(
            LocalDate from,
            LocalDate to) {
        int count = 0;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY
                    && d.getDayOfWeek()
                    != DayOfWeek.SUNDAY) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }
}