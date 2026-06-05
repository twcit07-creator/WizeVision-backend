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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * PAYMENT ENTITY
 *
 * Records money received against an invoice.
 * One invoice can have multiple payments
 * (partial payments are common in construction).
 *
 * payment_number format: PAY-2026-001
 */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(
                        name = "idx_payments_number",
                        columnList = "payment_number",
                        unique = true
                ),
                @Index(
                        name = "idx_payments_invoice",
                        columnList = "invoice_id"
                ),
                @Index(
                        name = "idx_payments_project",
                        columnList = "project_id"
                ),
                @Index(
                        name = "idx_payments_date",
                        columnList = "payment_date"
                ),
                @Index(
                        name = "idx_payments_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Column(
            name = "payment_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String paymentNumber;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(
            name = "amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "payment_date",
            nullable = false
    )
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_mode",
            nullable = false,
            length = 20
    )
    private PaymentMode paymentMode;

    /*
     * Transaction reference / cheque number / UTR number.
     * Mandatory for BANK_TRANSFER and CHEQUE.
     * Optional for others.
     */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "recorded_by_id")
    private UUID recordedById;
}