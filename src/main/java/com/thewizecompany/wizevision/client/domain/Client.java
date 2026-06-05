package com.thewizecompany.wizevision.client.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * CLIENT ENTITY
 *
 * Represents a fabricator company that gives
 * detailing projects to WizeVision.
 *
 * One client can have:
 * - Multiple contacts (people at that company)
 * - Multiple projects over time
 * - Multiple bids
 * - Multiple invoices
 *
 * company_code is a short human-readable identifier.
 * Format: WC-001, WC-002, etc.
 * Used in reports and bid documents.
 *
 * gst_number is the company tax ID.
 * Required for invoice generation.
 */
@Entity
@Table(
        name = "clients",
        indexes = {
                @Index(
                        name = "idx_clients_company_code",
                        columnList = "company_code",
                        unique = true
                ),
                @Index(
                        name = "idx_clients_company_name",
                        columnList = "company_name"
                ),
                @Index(
                        name = "idx_clients_is_active",
                        columnList = "is_active"
                ),
                @Index(
                        name = "idx_clients_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client extends BaseEntity {

    // ── IDENTITY ──────────────────────────────────────────────

    @Column(
            name = "company_code",
            nullable = false,
            unique = true,
            length = 20
    )
    private String companyCode;

    @Column(
            name = "company_name",
            nullable = false,
            length = 255
    )
    private String companyName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "website", length = 255)
    private String website;

    // ── ADDRESS ───────────────────────────────────────────────

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "pincode", length = 20)
    private String pincode;

    // ── BUSINESS DETAILS ──────────────────────────────────────

    /*
     * GST / Tax number.
     * Printed on all invoices sent to this client.
     * Optional — some clients may not have GST.
     */
    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    /*
     * Industry type.
     * Examples: Structural Steel, Industrial Building,
     * Commercial Building, Residential, Bridge, etc.
     * Helps categorize clients for marketing reports.
     */
    @Column(name = "industry_type", length = 100)
    private String industryType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── STATUS ────────────────────────────────────────────────

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── RELATIONSHIPS ─────────────────────────────────────────

    /*
     * One client has many contacts.
     * Loaded lazily — we do not need contacts
     * every time we load a client.
     *
     * CascadeType.ALL means:
     * If we delete a client, all their contacts
     * are also deleted (soft-deleted via BaseEntity).
     */
    @OneToMany(
            mappedBy = "client",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ClientContact> contacts = new ArrayList<>();
}