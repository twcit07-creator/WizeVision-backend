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

import java.time.Instant;
import java.util.UUID;

/*
 * LEAD ENTITY
 *
 * A potential client being tracked by the marketing team.
 *
 * A lead is NOT a client yet.
 * When a lead converts (has a real project):
 * 1. Lead status → CONVERTED
 * 2. Client record is created (or linked if already exists)
 * 3. ProjectInquiry is created and forwarded to PM
 *
 * lead_number format: LEAD-2026-001
 */
@Entity
@Table(
        name = "leads",
        indexes = {
                @Index(
                        name = "idx_leads_number",
                        columnList = "lead_number",
                        unique = true
                ),
                @Index(
                        name = "idx_leads_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_leads_assigned_to",
                        columnList = "assigned_to_id"
                ),
                @Index(
                        name = "idx_leads_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead extends BaseEntity {

    @Column(
            name = "lead_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String leadNumber;

    // ── COMPANY DETAILS ───────────────────────────────────────

    @Column(
            name = "company_name",
            nullable = false,
            length = 255
    )
    private String companyName;

    @Column(name = "industry_type", length = 100)
    private String industryType;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    // ── CONTACT DETAILS ───────────────────────────────────────

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_whatsapp", length = 20)
    private String contactWhatsapp;

    @Column(
            name = "contact_designation",
            length = 100
    )
    private String contactDesignation;

    // ── LEAD DETAILS ──────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source",
            nullable = false,
            length = 30
    )
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Column(
            name = "notes",
            columnDefinition = "TEXT"
    )
    private String notes;

    @Column(name = "lost_reason", length = 500)
    private String lostReason;

    // ── ASSIGNMENT ────────────────────────────────────────────

    /*
     * The marketing executive managing this lead.
     * UUID reference — not a JPA relationship
     * to keep the module loosely coupled.
     */
    @Column(
            name = "assigned_to_id",
            nullable = false
    )
    private UUID assignedToId;

    // ── CONVERSION ────────────────────────────────────────────

    /*
     * Set when lead converts to a client.
     * Links back to the clients table.
     */
    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "converted_at")
    private Instant convertedAt;
}