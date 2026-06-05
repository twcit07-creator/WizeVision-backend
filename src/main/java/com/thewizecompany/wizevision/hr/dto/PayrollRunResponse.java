package com.thewizecompany.wizevision.hr.dto;

import com.thewizecompany.wizevision.hr.domain.PayrollRun;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PayrollRunResponse {

    private final UUID id;
    private final Integer month;
    private final Integer year;
    private final String periodDisplay;
    private final PayrollRun.PayrollStatus status;
    private final Integer totalEmployees;
    private final BigDecimal totalGross;
    private final BigDecimal totalDeductions;
    private final BigDecimal totalNetPay;
    private final UUID runById;
    private final String runByName;
    private final Instant processedAt;
    private final Instant finalizedAt;
    private final String notes;
    private final Instant createdAt;
    private final List<PayslipResponse> payslips;
}