package com.thewizecompany.wizevision.invoicing.dto;

import com.thewizecompany.wizevision.invoicing.domain.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {

    private final UUID id;
    private final String invoiceNumber;

    private final UUID projectId;
    private final String projectNumber;
    private final String projectName;

    private final UUID clientId;
    private final String clientName;
    private final String clientCode;
    private final String clientGstNumber;

    private final UUID clientContactId;
    private final String clientContactName;
    private final String clientContactEmail;

    private final LocalDate invoiceDate;
    private final LocalDate dueDate;

    private final BigDecimal subtotal;
    private final BigDecimal taxPercentage;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;
    private final BigDecimal amountPaid;
    private final BigDecimal outstandingAmount;
    private final BigDecimal invoicedPercentage;

    private final InvoiceStatus status;
    private final String statusDisplay;
    private final boolean overdue;

    private final String lineItems;
    private final String notes;
    private final String termsAndConditions;

    private final Instant sentAt;
    private final Instant paidAt;
    private final Instant createdAt;
    private final String createdBy;

    private final List<PaymentResponse> payments;
}