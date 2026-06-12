package com.thewizecompany.wizevision.bidding.domain;

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
        name = "bids",
        indexes = {
                @Index(
                        name = "idx_bids_number",
                        columnList = "bid_number",
                        unique = true
                ),
                @Index(
                        name = "idx_bids_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_bids_client",
                        columnList = "client_id"
                ),
                @Index(
                        name = "idx_bids_created_by_pm",
                        columnList = "created_by_pm_id"
                ),
                @Index(
                        name = "idx_bids_inquiry",
                        columnList = "inquiry_id"
                ),
                @Index(
                        name = "idx_bids_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid extends BaseEntity {

    // ── IDENTITY ──────────────────────────────────────────────

    @Column(
            name = "bid_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String bidNumber;

    // ── LINKS ─────────────────────────────────────────────────

    @Column(name = "inquiry_id")
    private UUID inquiryId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "client_contact_id")
    private UUID clientContactId;

    // ── PM FILLS THESE ────────────────────────────────────────

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "project_location", length = 255)
    private String projectLocation;

    @Column(name = "scope_of_work", columnDefinition = "TEXT")
    private String scopeOfWork;

    /*
     * JSONB arrays — PM enters bullet points.
     * Stored as JSON array of strings.
     * Example: ["Shop drawings for all steel",
     *           "Anchor bolt plans",
     *           "Connection details"]
     */
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

    /*
     * Reference documents with dates.
     * Example: [{"type":"Structural Drawing",
     *            "date":"2026-01-10","revision":"Rev A"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "reference_documents",
            columnDefinition = "jsonb"
    )
    private String referenceDocuments;

    @Column(name = "estimated_weeks")
    private Integer estimatedWeeks;

    @Column(name = "proposed_start_date")
    private LocalDate proposedStartDate;

    @Column(name = "proposed_end_date")
    private LocalDate proposedEndDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── ADMIN FILLS THESE (hidden from PM) ───────────────────

    /*
     * Bid amount — only admin can see and set this.
     * PM response DTOs never include this field.
     * Null until admin fills it.
     */
    @Column(
            name = "bid_amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal bidAmount;

    /*
     * Admin's internal notes — visible to admin only.
     * Used for negotiation strategy, cost breakdown, etc.
     */
    @Column(
            name = "internal_notes",
            columnDefinition = "TEXT"
    )
    private String internalNotes;

    // ── STATUS & WORKFLOW ────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private BidStatus status = BidStatus.DRAFT;

    @Column(name = "created_by_pm_id", nullable = false)
    private UUID createdByPmId;

    @Column(name = "reviewed_by_admin_id")
    private UUID reviewedByAdminId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "sent_to_client_at")
    private Instant sentToClientAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /*
     * Tracks how many times bid was revised and resent.
     * Revision 0 = original bid
     * Revision 1 = first revision after negotiation
     */
    @Column(name = "revision_number", nullable = false)
    @Builder.Default
    private Integer revisionNumber = 0;

    // ── OUTCOME ───────────────────────────────────────────────

    /*
     * Set when bid is accepted.
     * Links to the project that was created.
     */
    @Column(name = "converted_project_id")
    private UUID convertedProjectId;
}