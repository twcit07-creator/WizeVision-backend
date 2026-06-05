package com.thewizecompany.wizevision.invoicing.domain;

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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/*
 * INVOICE ENTITY
 *
 * invoice_number format: INV-2026-001
 *
 * line_items JSONB:
 * [
 *   {
 *     "description": "Steel Detailing — Phase 1 (Modelling)",
 *     "quantity": 1,
 *     "unit": "lumpsum",
 *     "unitPrice": 50000,
 *     "amount": 50000
 *   },
 *   {
 *     "description": "Change Order CO-001 — Mezzanine Level",
 *     "quantity": 1,
 *     "unit": "lumpsum",
 *     "unitPrice": 12000,
 *     "amount": 12000
 *   }
 * ]
 */
@Entity
@Table(
        name = "invoices",
        indexes = {
                @Index(
                        name = "idx_invoices_number",
                        columnList = "invoice_number",
                        unique = true
                ),
                @Index(
                        name = "idx_invoices_project",
                        columnList = "project_id"
                ),
                @Index(
                        name = "idx_invoices_client",
                        columnList = "client_id"
                ),
                @Index(
                        name = "idx_invoices_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_invoices_due_date",
                        columnList = "due_date"
                ),
                @Index(
                        name = "idx_invoices_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends BaseEntity {

    @Column(
            name = "invoice_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String invoiceNumber;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_contact_id")
    private UUID clientContactId;

    // ── DATES ─────────────────────────────────────────────────

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // ── AMOUNTS ───────────────────────────────────────────────

    @Column(
            name = "subtotal",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal subtotal;

    /*
     * Tax percentage (GST, VAT, etc.)
     * Stored as percentage: 18.00 = 18%
     * 0 if not applicable.
     */
    @Column(
            name = "tax_percentage",
            precision = 5,
            scale = 2
    )
    @Builder.Default
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(
            name = "tax_amount",
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "amount_paid",
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    // ── STATUS ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // ── LINE ITEMS ────────────────────────────────────────────

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "line_items",
            columnDefinition = "jsonb"
    )
    private String lineItems;

    // ── CONTENT ───────────────────────────────────────────────

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(
            name = "terms_and_conditions",
            columnDefinition = "TEXT"
    )
    private String termsAndConditions;

    // ── TIMESTAMPS ────────────────────────────────────────────

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "paid_at")
    private Instant paidAt;


    /*
     * BILLING TARGET TYPE
     *
     * CONTRACT_BASE → this invoice is billed against
     *                 the main project contract amount
     * CHANGE_ORDER  → this invoice is billed against
     *                 a specific change order
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "target_type",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private InvoiceTargetType targetType =
            InvoiceTargetType.CONTRACT_BASE;

    /*
     * Links to the change order being billed.
     * Only set when targetType = CHANGE_ORDER.
     * Null for CONTRACT_BASE invoices.
     */
    @Column(name = "change_order_id")
    private UUID changeOrderId;

    /*
     * BILLING STATUS
     * PARTIAL       → more billing possible for this target
     * FULL_AND_FINAL → target fully billed, no more invoices
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "billing_status",
            length = 20
    )
    private BillingStatus billingStatus;

    /*
     * What percentage of the target this invoice represents.
     * Optional — stored for display on the invoice PDF.
     * Example: 30% advance, 70% final.
     */
    @Column(name = "billing_percentage", precision = 5, scale = 2)
    private BigDecimal billingPercentage;

    // ── HELPERS ───────────────────────────────────────────────

    public BigDecimal getOutstandingAmount() {
        return totalAmount.subtract(amountPaid)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isFullyPaid() {
        return amountPaid.compareTo(totalAmount) >= 0;
    }

    public boolean isOverdue() {
        return dueDate != null
                && LocalDate.now().isAfter(dueDate)
                && !isFullyPaid()
                && status != InvoiceStatus.CANCELLED;
    }

    /*
     * Calculates tax amount from subtotal and percentage.
     * Call this before saving when taxPercentage changes.
     */
    public void calculateAmounts() {
        if (taxPercentage != null
                && taxPercentage.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = subtotal
                    .multiply(taxPercentage)
                    .divide(
                            new BigDecimal("100"),
                            2,
                            RoundingMode.HALF_UP
                    );
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }
        this.totalAmount = subtotal.add(taxAmount);
    }
}