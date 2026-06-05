package com.thewizecompany.wizevision.invoicing.dto;

import com.thewizecompany.wizevision.invoicing.domain.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private final UUID id;
    private final String paymentNumber;
    private final UUID invoiceId;
    private final String invoiceNumber;
    private final UUID projectId;
    private final String projectNumber;
    private final BigDecimal amount;
    private final LocalDate paymentDate;
    private final PaymentMode paymentMode;
    private final String paymentModeDisplay;
    private final String referenceNumber;
    private final String notes;
    private final UUID recordedById;
    private final String recordedByName;
    private final Instant createdAt;
}