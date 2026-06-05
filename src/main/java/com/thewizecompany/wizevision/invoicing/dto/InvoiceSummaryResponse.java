package com.thewizecompany.wizevision.invoicing.dto;

import com.thewizecompany.wizevision.invoicing.domain.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * Lightweight response for lists.
 * No line items or payment details.
 */
@Getter
@Builder
public class InvoiceSummaryResponse {

    private final UUID id;
    private final String invoiceNumber;
    private final UUID projectId;
    private final String projectNumber;
    private final String projectName;
    private final UUID clientId;
    private final String clientName;
    private final LocalDate invoiceDate;
    private final LocalDate dueDate;
    private final BigDecimal totalAmount;
    private final BigDecimal amountPaid;
    private final BigDecimal outstandingAmount;
    private final InvoiceStatus status;
    private final String statusDisplay;
    private final boolean overdue;
}