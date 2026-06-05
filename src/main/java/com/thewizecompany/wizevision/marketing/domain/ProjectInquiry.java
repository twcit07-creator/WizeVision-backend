package com.thewizecompany.wizevision.marketing.domain;

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

import java.time.Instant;
import java.util.UUID;

/*
 * PROJECT INQUIRY ENTITY
 *
 * Created when a lead (or existing client) has a specific
 * project they want WizeVision to bid on.
 *
 * FLOW:
 * Marketing creates inquiry → attaches scope info
 * → forwards to PM → PM creates bid from this inquiry
 *
 * inquiry_number format: INQ-2026-001
 *
 * document_references: JSONB array of drawing references
 * Example:
 * [
 *   { "type": "Structural Drawing", "date": "2026-01-10",
 *     "revision": "Rev A" },
 *   { "type": "Architectural Drawing", "date": "2026-01-08",
 *     "revision": "Rev B" }
 * ]
 */
@Entity
@Table(
        name = "project_inquiries",
        indexes = {
                @Index(
                        name = "idx_inquiries_number",
                        columnList = "inquiry_number",
                        unique = true
                ),
                @Index(
                        name = "idx_inquiries_lead",
                        columnList = "lead_id"
                ),
                @Index(
                        name = "idx_inquiries_client",
                        columnList = "client_id"
                ),
                @Index(
                        name = "idx_inquiries_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_inquiries_forwarded_to",
                        columnList = "forwarded_to_id"
                ),
                @Index(
                        name = "idx_inquiries_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInquiry extends BaseEntity {

    @Column(
            name = "inquiry_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String inquiryNumber;

    // ── LINKS ─────────────────────────────────────────────────

    /*
     * lead_id is optional — an existing client can
     * directly submit a project inquiry without
     * going through the lead pipeline.
     */
    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "client_id")
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
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    /*
     * Drawing references stored as JSONB.
     * Array of { type, date, revision, notes }
     * Marketing team adds these when creating inquiry.
     * PM uses these when preparing the bid.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "document_references",
            columnDefinition = "jsonb"
    )
    private String documentReferences;

    // ── STATUS ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    // ── FORWARDING ────────────────────────────────────────────

    /*
     * PM who received this inquiry.
     * Set when marketing forwards it.
     */
    @Column(name = "forwarded_to_id")
    private UUID forwardedToId;

    @Column(name = "forwarded_by_id")
    private UUID forwardedById;

    @Column(name = "forwarded_at")
    private Instant forwardedAt;

    @Column(name = "forwarding_notes", length = 1000)
    private String forwardingNotes;

    // ── BID LINK ──────────────────────────────────────────────

    /*
     * Set when PM creates a bid from this inquiry.
     * Links to the bidding module.
     */
    @Column(name = "bid_id")
    private UUID bidId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}