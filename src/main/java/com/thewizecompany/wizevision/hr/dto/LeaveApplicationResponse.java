package com.thewizecompany.wizevision.hr.dto;

import com.thewizecompany.wizevision.hr.domain.LeaveStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class LeaveApplicationResponse {

    private final UUID id;
    private final String applicationNumber;
    private final UUID employeeId;
    private final String employeeName;
    private final String employeeCode;
    private final String department;
    private final UUID leaveTypeId;
    private final String leaveTypeName;
    private final String leaveTypeCode;
    private final boolean isPaid;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal totalDays;
    private final String reason;
    private final String supportingDocumentUrl;
    private final LeaveStatus status;
    private final String statusDisplay;
    private final UUID tlId;
    private final String tlName;
    private final Instant tlActionAt;
    private final String tlRemarks;
    private final UUID pmId;
    private final String pmName;
    private final Instant pmActionAt;
    private final String pmRemarks;
    private final UUID hrId;
    private final String hrName;
    private final Instant hrActionAt;
    private final String hrRemarks;
    private final Instant createdAt;
}