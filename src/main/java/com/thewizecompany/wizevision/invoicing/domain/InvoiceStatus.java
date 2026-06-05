package com.thewizecompany.wizevision.invoicing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvoiceStatus {

    DRAFT("Draft"),
    SENT("Sent to Client"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Fully Paid"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled");

    private final String displayName;
}