package com.thewizecompany.wizevision.hr.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class LeaveBalanceResponse {

    private final UUID leaveTypeId;
    private final String leaveTypeName;
    private final String leaveTypeCode;
    private final boolean isPaid;
    private final Integer year;
    private final BigDecimal totalDays;
    private final BigDecimal usedDays;
    private final BigDecimal pendingDays;
    private final BigDecimal carriedForwardDays;
    private final BigDecimal remainingDays;
}