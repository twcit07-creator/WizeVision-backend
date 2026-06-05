package com.thewizecompany.wizevision.projects.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "projects",
        indexes = {
                @Index(
                        name = "idx_projects_number",
                        columnList = "project_number",
                        unique = true
                ),
                @Index(
                        name = "idx_projects_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_projects_client",
                        columnList = "client_id"
                ),
                @Index(
                        name = "idx_projects_pm",
                        columnList = "pm_id"
                ),
                @Index(
                        name = "idx_projects_modeler",
                        columnList = "modeler_id"
                ),
                @Index(
                        name = "idx_projects_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project extends BaseEntity {

    // ── IDENTITY ──────────────────────────────────────────────

    /*
     * Format: J-TWC-2026-001
     * J    = Job
     * TWC  = The Wize Company
     * 2026 = year
     * 001  = sequence
     */
    @Column(
            name = "project_number",
            nullable = false,
            unique = true,
            length = 25
    )
    private String projectNumber;

    // ── LINKS ─────────────────────────────────────────────────

    @Column(name = "bid_id")
    private UUID bidId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_contact_id")
    private UUID clientContactId;

    // ── PROJECT DETAILS ───────────────────────────────────────

    @Column(
            name = "project_name",
            nullable = false,
            length = 255
    )
    private String projectName;

    @Column(name = "project_location", length = 255)
    private String projectLocation;

    @Column(
            name = "scope_of_work",
            columnDefinition = "TEXT"
    )
    private String scopeOfWork;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "inclusions",
            columnDefinition = "jsonb"
    )
    private String inclusions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "exclusions",
            columnDefinition = "jsonb"
    )
    private String exclusions;

    // ── FINANCIALS ────────────────────────────────────────────

    /*
     * contract_amount = original bid amount
     * change_orders_total = sum of all approved change orders
     * total_contract_value = contract_amount + change_orders_total
     * total_invoiced = sum of all invoices raised
     * total_paid = sum of all payments received
     */
    @Column(
            name = "contract_amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal contractAmount;

    @Column(
            name = "change_orders_total",
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal changeOrdersTotal = BigDecimal.ZERO;

    @Column(
            name = "total_invoiced",
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal totalInvoiced = BigDecimal.ZERO;

    @Column(
            name = "total_paid",
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    // ── STATUS & PHASE ────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "current_phase",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private ProjectPhase currentPhase = ProjectPhase.MODELLING;

    @Column(
            name = "progress_percentage",
            nullable = false
    )
    @Builder.Default
    private Integer progressPercentage = 0;

    // ── TEAM ──────────────────────────────────────────────────

    @Column(name = "pm_id", nullable = false)
    private UUID pmId;



    // ── TIMELINE ─────────────────────────────────────────────

    @Column(name = "estimated_start_date")
    private LocalDate estimatedStartDate;

    @Column(name = "estimated_end_date")
    private LocalDate estimatedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "estimated_weeks")
    private Integer estimatedWeeks;

    // ── NOTES ─────────────────────────────────────────────────

    @Column(
            name = "pm_notes",
            columnDefinition = "TEXT"
    )
    private String pmNotes;

    @Column(
            name = "on_hold_reason",
            length = 500
    )
    private String onHoldReason;

    @Column(name = "on_hold_at")
    private Instant onHoldAt;

    // ── HELPERS ───────────────────────────────────────────────

    public BigDecimal getTotalContractValue() {
        return contractAmount != null
                ? contractAmount.add(changeOrdersTotal)
                : changeOrdersTotal;
    }

    public BigDecimal getOutstandingAmount() {
        return getTotalContractValue()
                .subtract(totalPaid);
    }

    /*
     * Team is considered assigned when there is at least
     * one active MODELER, one EDITOR, and one CHECKER.
     * Team leads are optional.
     */
    public boolean isTeamAssigned() {
        return false; // Will be determined by service layer
        // checking ProjectAssignment records
    }
}