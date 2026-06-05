package com.thewizecompany.wizevision.invoicing.dto;

import com.thewizecompany.wizevision.invoicing.domain.InvoiceTargetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * CREATE INVOICE REQUEST — BillNova pattern
 *
 * An invoice targets either:
 *   A) The base contract amount (CONTRACT_BASE)
 *   B) A specific change order  (CHANGE_ORDER)
 *
 * Amount can be specified as:
 *   A) A fixed amount (amount field)
 *   B) A percentage of the target (percentage field)
 *      → System calculates: target * percentage / 100
 *
 * Over-billing protection:
 *   Sum of all existing invoices for this target
 *   + new invoice amount must NOT exceed target amount.
 *   If it would → request is REJECTED with clear message.
 */
@Getter
@Setter
public class CreateInvoiceRequest {

    @NotNull(message = "Project is required")
    private UUID projectId;

    @NotNull(message = "Invoice target type is required")
    private InvoiceTargetType targetType;

    /*
     * Required when targetType = CHANGE_ORDER.
     * Must be null when targetType = CONTRACT_BASE.
     */
    private UUID changeOrderId;

    private UUID clientContactId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    /*
     * Provide EITHER amount OR percentage, not both.
     * If percentage is provided and > 0, amount is ignored
     * and calculated automatically.
     */
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @DecimalMin(value = "0.01")
    private BigDecimal percentage;

    private List<LineItemDto> lineItems;

    @DecimalMin(value = "0")
    private BigDecimal taxPercentage;

    private String notes;
    private String termsAndConditions;

    @Getter
    @Setter
    public static class LineItemDto {
        private String description;
        private Integer quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal amount;
    }
}