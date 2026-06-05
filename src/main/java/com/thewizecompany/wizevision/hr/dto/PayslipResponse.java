package com.thewizecompany.wizevision.hr.dto;

import com.thewizecompany.wizevision.hr.domain.Payslip;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class PayslipResponse {

    private final UUID id;
    private final UUID payrollRunId;
    private final String payrollPeriod;

    private final UUID employeeId;
    private final String employeeCode;
    private final String employeeName;
    private final String department;
    private final String designation;

    private final Integer month;
    private final Integer year;

    private final Integer totalWorkingDays;
    private final BigDecimal presentDays;
    private final BigDecimal paidLeaveDays;
    private final BigDecimal unpaidLeaveDays;
    private final BigDecimal overtimeHours;

    private final String earnings;
    private final String deductions;

    private final BigDecimal grossEarnings;
    private final BigDecimal totalDeductions;
    private final BigDecimal netPay;

    private final BigDecimal pfEmployee;
    private final BigDecimal pfEmployer;
    private final BigDecimal esiEmployee;
    private final BigDecimal esiEmployer;
    private final BigDecimal professionalTax;
    private final BigDecimal tds;

    private final Payslip.PayslipStatus status;
    private final LocalDate paymentDate;
    private final String paymentReference;
}