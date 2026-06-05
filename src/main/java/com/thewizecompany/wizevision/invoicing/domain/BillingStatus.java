package com.thewizecompany.wizevision.invoicing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingStatus {

    /*
     * PARTIAL       → Invoice covers part of the target amount.
     *                 More invoices can be raised for same target.
     * FULL_AND_FINAL → Invoice brings total billed to exactly
     *                  the target amount.
     *                  No more invoices allowed for this target.
     */
    PARTIAL("Partial Billing"),
    FULL_AND_FINAL("Full and Final");

    private final String displayName;
}