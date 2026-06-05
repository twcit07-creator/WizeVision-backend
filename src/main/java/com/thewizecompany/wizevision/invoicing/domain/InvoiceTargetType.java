package com.thewizecompany.wizevision.invoicing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvoiceTargetType {

    /*
     * CONTRACT_BASE  → Invoice is against the main contract amount
     * CHANGE_ORDER   → Invoice is against a specific change order
     */
    CONTRACT_BASE("Base Contract"),
    CHANGE_ORDER("Change Order");

    private final String displayName;
}