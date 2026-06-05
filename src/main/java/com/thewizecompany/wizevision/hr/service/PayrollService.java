package com.thewizecompany.wizevision.hr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thewizecompany.wizevision.attendance.repository.AttendanceRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.domain.SalaryStructure;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.hr.domain.PayrollRun;
import com.thewizecompany.wizevision.hr.domain.Payslip;
import com.thewizecompany.wizevision.hr.domain.SalaryComponent;
import com.thewizecompany.wizevision.hr.dto.AssignSalaryStructureRequest;
import com.thewizecompany.wizevision.hr.dto.InitiatePayrollRequest;
import com.thewizecompany.wizevision.hr.dto.PayrollRunResponse;
import com.thewizecompany.wizevision.hr.dto.PayslipResponse;
import com.thewizecompany.wizevision.hr.repository.EmployeeSalaryStructureRepository;
import com.thewizecompany.wizevision.hr.repository.PayrollRunRepository;
import com.thewizecompany.wizevision.hr.repository.PayslipRepository;
import com.thewizecompany.wizevision.hr.repository.SalaryComponentRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final BigDecimal PF_RATE =
            new BigDecimal("12.00");
    private static final BigDecimal ESI_RATE_EMPLOYEE =
            new BigDecimal("0.75");
    private static final BigDecimal ESI_RATE_EMPLOYER =
            new BigDecimal("3.25");
    private static final BigDecimal ESI_GROSS_LIMIT =
            new BigDecimal("21000");
    private static final BigDecimal PROFESSIONAL_TAX =
            new BigDecimal("200");

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryStructureRepository
            salaryStructureRepository;
    private final SalaryComponentRepository componentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────
    // ASSIGN SALARY STRUCTURE TO EMPLOYEE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void assignSalaryStructure(
            UUID employeeId,
            AssignSalaryStructureRequest request) {

        employeeRepository
                .findByIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee", employeeId.toString()
                        )
                );

        /*
         * Deactivate the current structure.
         */
        salaryStructureRepository
                .findByEmployeeIdAndIsActiveTrueAndIsDeletedFalse(
                        employeeId
                )
                .ifPresent(existing -> {
                    existing.setActive(false);
                    existing.setEffectiveTo(
                            request.getEffectiveFrom().minusDays(1)
                    );
                    salaryStructureRepository.save(existing);
                });

        /*
         * Build the components JSON.
         * Resolve each component and calculate amounts.
         */
        List<Map<String, Object>> componentsList =
                new ArrayList<>();

        BigDecimal basicAmount = BigDecimal.ZERO;

        /*
         * First pass — find BASIC to use in percentage calcs.
         */
        for (var cv : request.getComponents()) {
            var component = componentRepository
                    .findByIdAndIsDeletedFalse(cv.getComponentId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "SalaryComponent",
                                    cv.getComponentId().toString()
                            )
                    );

            if (component.getCode().equals("BASIC")) {
                basicAmount = cv.getValue();
                break;
            }
        }

        /*
         * Second pass — calculate all components.
         */
        for (var cv : request.getComponents()) {
            var component = componentRepository
                    .findByIdAndIsDeletedFalse(cv.getComponentId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "SalaryComponent",
                                    cv.getComponentId().toString()
                            )
                    );

            BigDecimal amount = calculateComponentAmount(
                    component, cv.getValue(), basicAmount,
                    BigDecimal.ZERO
            );

            Map<String, Object> comp = new HashMap<>();
            comp.put("componentId",
                    component.getId().toString());
            comp.put("componentCode", component.getCode());
            comp.put("componentName", component.getName());
            comp.put("type", component.getType().name());
            comp.put("calculationType",
                    component.getCalculationType().name());
            comp.put("value", cv.getValue());
            comp.put("amount", amount);
            componentsList.add(comp);
        }

        String componentsJson;
        try {
            componentsJson = objectMapper
                    .writeValueAsString(componentsList);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "Failed to save salary structure",
                    "SERIALIZATION_ERROR"
            );
        }

        var newStructure =
                com.thewizecompany.wizevision.hr.domain
                        .EmployeeSalaryStructure.builder()
                        .employeeId(employeeId)
                        .effectiveFrom(request.getEffectiveFrom())
                        .components(componentsJson)
                        .isActive(true)
                        .notes(request.getNotes())
                        .build();

        salaryStructureRepository.save(newStructure);

        log.info(
                "Salary structure assigned to employee: {}",
                employeeId
        );
    }

    // ─────────────────────────────────────────────────────────
    // INITIATE PAYROLL RUN
    // ─────────────────────────────────────────────────────────

    @Transactional
    public PayrollRunResponse initiatePayrollRun(
            InitiatePayrollRequest request,
            UUID runById) {

        if (payrollRunRepository
                .findByMonthAndYearAndIsDeletedFalse(
                        request.getMonth(),
                        request.getYear()
                )
                .isPresent()) {
            throw new BusinessException(
                    "Payroll run already exists for " +
                            request.getMonth() + "/" + request.getYear(),
                    "PAYROLL_RUN_EXISTS"
            );
        }

        PayrollRun run = PayrollRun.builder()
                .month(request.getMonth())
                .year(request.getYear())
                .status(PayrollRun.PayrollStatus.DRAFT)
                .runById(runById)
                .notes(request.getNotes())
                .build();

        PayrollRun saved = payrollRunRepository.save(run);

        log.info(
                "Payroll run initiated for {}/{}",
                request.getMonth(),
                request.getYear()
        );

        return mapRunToResponse(saved, List.of());
    }

    // ─────────────────────────────────────────────────────────
    // PROCESS PAYROLL
    // ─────────────────────────────────────────────────────────

    @Transactional
    public PayrollRunResponse processPayroll(UUID runId) {
        PayrollRun run = findRun(runId);

        if (run.getStatus() != PayrollRun.PayrollStatus.DRAFT) {
            throw new BusinessException(
                    "Payroll run must be in DRAFT status to process",
                    "INVALID_RUN_STATUS"
            );
        }

        run.setStatus(PayrollRun.PayrollStatus.PROCESSING);
        payrollRunRepository.save(run);

        /*
         * Get all active employees.
         */
        List<Employee> employees = employeeRepository
                .findAll()
                .stream()
                .filter(e -> !e.isDeleted() && e.isActive())
                .toList();

        List<Payslip> payslips = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNetPay = BigDecimal.ZERO;

        YearMonth yearMonth = YearMonth.of(
                run.getYear(), run.getMonth()
        );
        int workingDays = calculateWorkingDays(yearMonth);

        for (Employee employee : employees) {
            try {
                Payslip payslip = generatePayslip(
                        employee, run, workingDays, yearMonth
                );
                payslips.add(payslip);
                assert payslip != null;
                totalGross = totalGross.add(
                        payslip.getGrossEarnings()
                );
                totalDeductions = totalDeductions.add(
                        payslip.getTotalDeductions()
                );
                totalNetPay = totalNetPay.add(
                        payslip.getNetPay()
                );
            } catch (Exception e) {
                log.error(
                        "Failed to generate payslip for {}: {}",
                        employee.getEmployeeCode(),
                        e.getMessage()
                );
            }
        }

        run.setStatus(PayrollRun.PayrollStatus.PROCESSED);
        run.setProcessedAt(Instant.now());
        run.setTotalEmployees(payslips.size());
        run.setTotalGross(totalGross);
        run.setTotalDeductions(totalDeductions);
        run.setTotalNetPay(totalNetPay);

        PayrollRun savedRun = payrollRunRepository.save(run);

        log.info(
                "Payroll processed: {}/{} — {} employees — " +
                        "Net pay: {}",
                run.getMonth(),
                run.getYear(),
                payslips.size(),
                totalNetPay
        );

        return mapRunToResponse(
                savedRun,
                payslips.stream()
                        .map(p -> mapPayslipToResponse(p, savedRun))
                        .toList()
        );
    }

    // ─────────────────────────────────────────────────────────
    // FINALIZE PAYROLL
    // ─────────────────────────────────────────────────────────

    @Transactional
    public PayrollRunResponse finalizePayroll(UUID runId) {
        PayrollRun run = findRun(runId);

        if (run.getStatus()
                != PayrollRun.PayrollStatus.PROCESSED) {
            throw new BusinessException(
                    "Payroll must be PROCESSED before finalizing",
                    "INVALID_RUN_STATUS"
            );
        }

        run.setStatus(PayrollRun.PayrollStatus.FINALIZED);
        run.setFinalizedAt(Instant.now());

        payslipRepository
                .findByPayrollRunIdAndIsDeletedFalse(runId)
                .forEach(payslip -> {
                    payslip.setStatus(Payslip.PayslipStatus.FINALIZED);
                    payslipRepository.save(payslip);
                });

        PayrollRun savedRun = payrollRunRepository.save(run);

        log.info(
                "Payroll finalized: {}/{}",
                run.getMonth(),
                run.getYear()
        );

        return mapRunToResponse(savedRun, List.of());
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PayslipResponse getMyPayslip(
            UUID employeeId,
            Integer month,
            Integer year) {

        var payslip = payslipRepository
                .findByEmployeeIdAndMonthAndYearAndIsDeletedFalse(
                        employeeId, month, year
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payslip",
                                "Employee " + employeeId +
                                        " for " + month + "/" + year
                        )
                );

        var run = findRun(payslip.getPayrollRunId());
        return mapPayslipToResponse(payslip, run);
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> getMyPayslips(
            UUID employeeId) {

        return payslipRepository
                .findByEmployeeIdAndIsDeletedFalseOrderByYearDescMonthDesc(
                        employeeId
                )
                .stream()
                .map(p -> {
                    var run = findRun(p.getPayrollRunId());
                    return mapPayslipToResponse(p, run);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getAllRuns() {
        return payrollRunRepository
                .findByIsDeletedFalseOrderByYearDescMonthDesc()
                .stream()
                .map(run -> mapRunToResponse(run, List.of()))
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────

    private Payslip generatePayslip(
            Employee employee,
            PayrollRun run,
            int workingDays,
            YearMonth yearMonth) {

        /*
         * Get employee's salary structure.
         */
        var structure = salaryStructureRepository
                .findByEmployeeIdAndIsActiveTrueAndIsDeletedFalse(
                        employee.getId()
                )
                .orElse(null);

        if (structure == null) {
            log.warn(
                    "No salary structure for employee: {}",
                    employee.getEmployeeCode()
            );
            return null;
        }

        /*
         * Get attendance for this month.
         */
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        var attendanceRecords = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenAndIsDeletedFalse(
                        employee.getId(), monthStart, monthEnd
                );

        BigDecimal presentDays = BigDecimal.valueOf(
                attendanceRecords.stream()
                        .filter(r -> r.getCheckInTime() != null)
                        .count()
        );

        double totalOvertimeMinutes = attendanceRecords
                .stream()
                .mapToInt(r -> r.getOvertimeMinutes() != null
                        ? r.getOvertimeMinutes() : 0)
                .sum();
        BigDecimal overtimeHours = BigDecimal.valueOf(totalOvertimeMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        /*
         * Get approved leave for this month.
         */
        BigDecimal paidLeaveDays = BigDecimal.ZERO;
        BigDecimal unpaidLeaveDays = BigDecimal.ZERO;

        /*
         * Parse components and calculate amounts.
         */
        List<Map<String, Object>> componentsList;
        try {
            componentsList = objectMapper.readValue(
                    structure.getComponents(),
                    new TypeReference<>() {}
            );
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to parse salary structure: {}",
                    e.getMessage()
            );
            return null;
        }

        /*
         * Calculate per-day rate for attendance-based calcs.
         * Per day = monthly salary / working days in month
         */
        BigDecimal basicSalary = BigDecimal.ZERO;
        for (var comp : componentsList) {
            if ("BASIC".equals(comp.get("componentCode"))) {
                basicSalary = new BigDecimal(
                        comp.get("amount").toString()
                );
                break;
            }
        }

        BigDecimal perDayRate = workingDays > 0
                ? basicSalary.divide(
                BigDecimal.valueOf(workingDays),
                2,
                RoundingMode.HALF_UP
        )
                : BigDecimal.ZERO;

        BigDecimal effectiveDays = presentDays.add(paidLeaveDays);

        BigDecimal unpaidLeaveDeduction = BigDecimal.valueOf(workingDays);

        /*
         * Calculate gross earnings.
         */
        List<Map<String, Object>> earnings = new ArrayList<>();
        List<Map<String, Object>> deductions = new ArrayList<>();
        BigDecimal grossEarnings = BigDecimal.ZERO;

        /*
         * Prorate salary if attendance is less than
         * working days.
         */
        BigDecimal prorationFactor = workingDays > 0
                ? effectiveDays.divide(
                BigDecimal.valueOf(workingDays),
                4,
                RoundingMode.HALF_UP
        )
                : BigDecimal.ONE;

        for (var comp : componentsList) {
            String type = (String) comp.get("type");
            if (!"EARNING".equals(type)) continue;

            BigDecimal amount = new BigDecimal(
                    comp.get("amount").toString()
            ).multiply(prorationFactor)
                    .setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> earning = new HashMap<>(comp);
            earning.put("amount", amount);
            earnings.add(earning);
            grossEarnings = grossEarnings.add(amount);
        }

        /*
         * Calculate deductions.
         */
        BigDecimal pfEmployee = BigDecimal.ZERO;
        BigDecimal pfEmployer = BigDecimal.ZERO;
        BigDecimal esiEmployee = BigDecimal.ZERO;
        BigDecimal esiEmployer = BigDecimal.ZERO;
        BigDecimal professionalTax = BigDecimal.ZERO;
        BigDecimal totalDeductionsAmount = BigDecimal.ZERO;

        BigDecimal proratedBasic = basicSalary
                .multiply(prorationFactor)
                .setScale(2, RoundingMode.HALF_UP);

        /*
         * PF deduction.
         */
        if (employee.isPfApplicable()) {
            pfEmployee = proratedBasic
                    .multiply(PF_RATE)
                    .divide(
                            new BigDecimal("100"),
                            2,
                            RoundingMode.HALF_UP
                    );
            pfEmployer = pfEmployee;

            Map<String, Object> pfDed = new HashMap<>();
            pfDed.put("componentCode", "PF_EMP");
            pfDed.put("componentName", "PF (Employee)");
            pfDed.put("type", "DEDUCTION");
            pfDed.put("amount", pfEmployee);
            deductions.add(pfDed);
            totalDeductionsAmount = totalDeductionsAmount
                    .add(pfEmployee);
        }

        /*
         * ESI deduction (only if gross < 21000).
         */
        if (employee.isEsiApplicable()
                && grossEarnings.compareTo(
                ESI_GROSS_LIMIT) <= 0) {
            esiEmployee = grossEarnings
                    .multiply(ESI_RATE_EMPLOYEE)
                    .divide(
                            new BigDecimal("100"),
                            2,
                            RoundingMode.HALF_UP
                    );
            esiEmployer = grossEarnings
                    .multiply(ESI_RATE_EMPLOYER)
                    .divide(
                            new BigDecimal("100"),
                            2,
                            RoundingMode.HALF_UP
                    );

            Map<String, Object> esiDed = new HashMap<>();
            esiDed.put("componentCode", "ESI_EMP");
            esiDed.put("componentName", "ESI (Employee)");
            esiDed.put("type", "DEDUCTION");
            esiDed.put("amount", esiEmployee);
            deductions.add(esiDed);
            totalDeductionsAmount = totalDeductionsAmount
                    .add(esiEmployee);
        }

        /*
         * Professional Tax (Maharashtra: 200/month).
         */
        if (grossEarnings.compareTo(
                new BigDecimal("10000")) > 0) {
            professionalTax = PROFESSIONAL_TAX;

            Map<String, Object> ptDed = new HashMap<>();
            ptDed.put("componentCode", "PT");
            ptDed.put("componentName", "Professional Tax");
            ptDed.put("type", "DEDUCTION");
            ptDed.put("amount", professionalTax);
            deductions.add(ptDed);
            totalDeductionsAmount = totalDeductionsAmount
                    .add(professionalTax);
        }

        /*
         * Leave deduction for unpaid leave.
         */
        if (unpaidLeaveDeduction.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal leaveDeduction = perDayRate
                    .multiply(
                            unpaidLeaveDeduction
                    )
                    .setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> lvDed = new HashMap<>();
            lvDed.put("componentCode", "LEAVE_DED");
            lvDed.put("componentName", "Leave Deduction");
            lvDed.put("type", "DEDUCTION");
            lvDed.put("amount", leaveDeduction);
            deductions.add(lvDed);
            totalDeductionsAmount = totalDeductionsAmount
                    .add(leaveDeduction);
        }

        BigDecimal netPay = grossEarnings
                .subtract(totalDeductionsAmount)
                .max(BigDecimal.ZERO);

        String earningsJson;
        String deductionsJson;
        try {
            earningsJson = objectMapper
                    .writeValueAsString(earnings);
            deductionsJson = objectMapper
                    .writeValueAsString(deductions);
        } catch (JsonProcessingException e) {
            earningsJson = "[]";
            deductionsJson = "[]";
        }

        /*
         * Check if payslip already exists.
         * If so, update it.
         */
        var existingOpt = payslipRepository
                .findByEmployeeIdAndMonthAndYearAndIsDeletedFalse(
                        employee.getId(),
                        run.getMonth(),
                        run.getYear()
                );

        Payslip payslip;

        if (existingOpt.isPresent()) {
            payslip = existingOpt.get();
        } else {
            payslip = new Payslip();
            payslip.setPayrollRunId(run.getId());
            payslip.setEmployeeId(employee.getId());
            payslip.setMonth(run.getMonth());
            payslip.setYear(run.getYear());
        }

        payslip.setTotalWorkingDays(workingDays);
        payslip.setPresentDays(presentDays);
        payslip.setPaidLeaveDays(paidLeaveDays);
        payslip.setUnpaidLeaveDays(unpaidLeaveDeduction);
        payslip.setOvertimeHours(overtimeHours);
        payslip.setEarnings(earningsJson);
        payslip.setDeductions(deductionsJson);
        payslip.setGrossEarnings(grossEarnings);
        payslip.setTotalDeductions(totalDeductionsAmount);
        payslip.setNetPay(netPay);
        payslip.setPfEmployee(pfEmployee);
        payslip.setPfEmployer(pfEmployer);
        payslip.setEsiEmployee(esiEmployee);
        payslip.setEsiEmployer(esiEmployer);
        payslip.setProfessionalTax(professionalTax);
        payslip.setStatus(Payslip.PayslipStatus.DRAFT);

        return payslipRepository.save(payslip);
    }

    private BigDecimal calculateComponentAmount(
            SalaryComponent component,
            BigDecimal value,
            BigDecimal basicAmount,
            BigDecimal grossAmount) {

        return switch (component.getCalculationType()) {
            case FIXED -> value;
            case PERCENTAGE_OF_BASIC ->
                    basicAmount
                            .multiply(value)
                            .divide(
                                    new BigDecimal("100"),
                                    2,
                                    RoundingMode.HALF_UP
                            );
            case PERCENTAGE_OF_GROSS ->
                    grossAmount
                            .multiply(value)
                            .divide(
                                    new BigDecimal("100"),
                                    2,
                                    RoundingMode.HALF_UP
                            );
            case ATTENDANCE_BASED -> BigDecimal.ZERO;
        };
    }

    private int calculateWorkingDays(YearMonth yearMonth) {
        int count = 0;
        LocalDate date = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        while (!date.isAfter(end)) {
            if (date.getDayOfWeek()
                    != java.time.DayOfWeek.SATURDAY
                    && date.getDayOfWeek()
                    != java.time.DayOfWeek.SUNDAY) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    private PayrollRun findRun(UUID id) {
        return payrollRunRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "PayrollRun", id.toString()
                        )
                );
    }

    private PayrollRunResponse mapRunToResponse(
            PayrollRun run,
            List<PayslipResponse> payslips) {

        String runByName = run.getRunById() != null
                ? employeeRepository
                  .findByIdAndIsDeletedFalse(run.getRunById())
                  .map(Employee::getFullName)
                  .orElse(null)
                : null;

        return PayrollRunResponse.builder()
                .id(run.getId())
                .month(run.getMonth())
                .year(run.getYear())
                .periodDisplay(run.getPeriodDisplay())
                .status(run.getStatus())
                .totalEmployees(run.getTotalEmployees())
                .totalGross(run.getTotalGross())
                .totalDeductions(run.getTotalDeductions())
                .totalNetPay(run.getTotalNetPay())
                .runById(run.getRunById())
                .runByName(runByName)
                .processedAt(run.getProcessedAt())
                .finalizedAt(run.getFinalizedAt())
                .notes(run.getNotes())
                .createdAt(run.getCreatedAt())
                .payslips(payslips)
                .build();
    }

    private PayslipResponse mapPayslipToResponse(
            Payslip payslip,
            PayrollRun run) {

        var employee = employeeRepository
                .findByIdAndIsDeletedFalse(payslip.getEmployeeId())
                .orElse(null);

        return PayslipResponse.builder()
                .id(payslip.getId())
                .payrollRunId(payslip.getPayrollRunId())
                .payrollPeriod(run.getPeriodDisplay())
                .employeeId(payslip.getEmployeeId())
                .employeeCode(employee != null
                        ? employee.getEmployeeCode() : null)
                .employeeName(employee != null
                        ? employee.getFullName() : null)
                .department(employee != null
                        && employee.getDepartment() != null
                        ? employee.getDepartment().getName() : null)
                .designation(employee != null
                        && employee.getDesignation() != null
                        ? employee.getDesignation().getTitle() : null)
                .month(payslip.getMonth())
                .year(payslip.getYear())
                .totalWorkingDays(payslip.getTotalWorkingDays())
                .presentDays(payslip.getPresentDays())
                .paidLeaveDays(payslip.getPaidLeaveDays())
                .unpaidLeaveDays(payslip.getUnpaidLeaveDays())
                .overtimeHours(payslip.getOvertimeHours())
                .earnings(payslip.getEarnings())
                .deductions(payslip.getDeductions())
                .grossEarnings(payslip.getGrossEarnings())
                .totalDeductions(payslip.getTotalDeductions())
                .netPay(payslip.getNetPay())
                .pfEmployee(payslip.getPfEmployee())
                .pfEmployer(payslip.getPfEmployer())
                .esiEmployee(payslip.getEsiEmployee())
                .esiEmployer(payslip.getEsiEmployer())
                .professionalTax(payslip.getProfessionalTax())
                .tds(payslip.getTds())
                .status(payslip.getStatus())
                .paymentDate(payslip.getPaymentDate())
                .paymentReference(payslip.getPaymentReference())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SalaryComponent> getSalaryComponents() {
        return componentRepository
                .findByIsActiveTrueAndIsDeletedFalse();
    }
}