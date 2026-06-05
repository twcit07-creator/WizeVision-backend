package com.thewizecompany.wizevision.projects.dto;

import com.thewizecompany.wizevision.projects.domain.ChangeOrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChangeOrderResponse {

    private final UUID id;
    private final String changeOrderNumber;
    private final UUID projectId;
    private final String projectNumber;
    private final String projectName;
    private final String description;
    private final String scopeOfChange;
    private final ChangeOrderStatus status;
    private final String statusDisplay;
    private final BigDecimal amount;
    private final UUID createdByPmId;
    private final String createdByPmName;
    private final Instant submittedAt;
    private final UUID approvedById;
    private final String approvedByName;
    private final Instant approvedAt;
    private final String rejectionReason;
    private final String notes;
    private final Instant createdAt;
}