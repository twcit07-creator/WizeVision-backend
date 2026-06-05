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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/*
 * CHANGE ORDER
 *
 * Additional work requested by client after
 * the main project contract is signed.
 *
 * Number format: {projectNumber}-COR-{sequence}
 * Example: J-TWC-2026-001-COR-001
 *
 * WORKFLOW (mirrors the main bid workflow):
 * PM describes the extra work → DRAFT
 * PM submits to Admin         → SUBMITTED
 * Admin sets price            → (price set)
 * Admin approves              → APPROVED
 *   → project.changeOrdersTotal += amount
 * Admin rejects               → REJECTED
 */
@Entity
@Table(
        name = "change_orders",
        indexes = {
                @Index(
                        name = "idx_change_orders_number",
                        columnList = "change_order_number",
                        unique = true
                ),
                @Index(
                        name = "idx_change_orders_project",
                        columnList = "project_id"
                ),
                @Index(
                        name = "idx_change_orders_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeOrder extends BaseEntity {

    /*
     * Format: J-TWC-2026-001-COR-001
     * Derived from project number.
     */
    @Column(
            name = "change_order_number",
            nullable = false,
            unique = true,
            length = 35
    )
    private String changeOrderNumber;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "scope_of_change",
            columnDefinition = "TEXT"
    )
    private String scopeOfChange;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private ChangeOrderStatus status = ChangeOrderStatus.DRAFT;

    /*
     * Amount set by admin (same pattern as bid amount).
     * PM cannot set this.
     */
    @Column(
            name = "amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(name = "created_by_pm_id", nullable = false)
    private UUID createdByPmId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_by_id")
    private UUID approvedById;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}