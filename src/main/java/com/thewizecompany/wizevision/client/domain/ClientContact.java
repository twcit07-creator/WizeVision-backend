package com.thewizecompany.wizevision.client.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * CLIENT CONTACT ENTITY
 *
 * A person who works at a client company.
 * This is the person your marketing team
 * actually speaks to.
 *
 * One client can have multiple contacts:
 * - The main procurement manager
 * - The technical engineer
 * - The accounts person (for invoice queries)
 *
 * is_primary = true means this is the main contact.
 * Only ONE contact per client should be primary.
 * Enforced in service layer.
 *
 * When creating a bid, PM selects:
 * - Which client company
 * - Which contact person at that company
 */
@Entity
@Table(
        name = "client_contacts",
        indexes = {
                @Index(
                        name = "idx_client_contacts_client",
                        columnList = "client_id"
                ),
                @Index(
                        name = "idx_client_contacts_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_client_contacts_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "client_id",
            nullable = false
    )
    private Client client;

    // ── PERSONAL DETAILS ──────────────────────────────────────

    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    /*
     * Their job title at the client company.
     * Example: "Procurement Manager", "Project Engineer"
     * Different from our system's Designation entity —
     * this is the contact's title at THEIR company.
     */
    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    /*
     * WhatsApp number — common in construction industry
     * for quick communication about project details.
     * May differ from phone.
     */
    @Column(name = "whatsapp", length = 20)
    private String whatsapp;

    @Column(name = "notes", length = 1000)
    private String notes;

    // ── STATUS ────────────────────────────────────────────────

    /*
     * Primary contact for this client.
     * Used when sending communications —
     * if no specific contact is chosen,
     * the primary contact is used.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── HELPER ────────────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }
}