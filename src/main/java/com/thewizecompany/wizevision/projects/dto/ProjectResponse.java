package com.thewizecompany.wizevision.projects.dto;

import com.thewizecompany.wizevision.projects.domain.ProjectPhase;
import com.thewizecompany.wizevision.projects.domain.ProjectStatus;
import lombok.*;



import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@Builder
public class ProjectResponse {

    private final UUID id;
    private final String projectNumber;

    private final UUID bidId;
    private final UUID clientId;
    private final String clientName;
    private final String clientCode;

    private final UUID clientContactId;
    private final String clientContactName;

    private final String projectName;
    private final String projectLocation;
    private final String scopeOfWork;
    private final String inclusions;
    private final String exclusions;

    /*
     * Financial fields — only populated for ADMIN/SUPER_ADMIN.
     * For PM and other roles these will be null.
     * Frontend checks: if contractAmount is null → hide finances.
     */
    private final BigDecimal contractAmount;
    private final BigDecimal changeOrdersTotal;
    private final BigDecimal totalContractValue;
    private final BigDecimal totalInvoiced;
    private final BigDecimal totalPaid;
    private final BigDecimal outstandingAmount;

    private final ProjectStatus status;
    private final String statusDisplay;
    private final ProjectPhase currentPhase;
    private final String phaseDisplay;
    private final Integer progressPercentage;

    private final UUID pmId;
    private final String pmName;
    private final List<TeamMemberResponse> team;
    private final boolean teamAssigned;

    private final LocalDate estimatedStartDate;
    private final LocalDate estimatedEndDate;
    private final LocalDate actualStartDate;
    private final LocalDate actualEndDate;
    private final Integer estimatedWeeks;

    private final String pmNotes;
    private final String onHoldReason;
    private final Instant onHoldAt;

    private final long changeOrdersCount;
    private final Instant createdAt;
    private final String createdBy;

    @Getter
    @Builder
    public static class TeamMemberResponse {
        private final UUID employeeId;
        private final String employeeCode;
        private final String fullName;
        private final String role;
        private final String roleDisplay;
        private final Instant assignedAt;
        private final String assignedBy;
    }
}