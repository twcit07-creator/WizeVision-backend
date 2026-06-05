package com.thewizecompany.wizevision.invoicing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMode {

    BANK_TRANSFER("Bank Transfer"),
    CHEQUE("Cheque"),
    CASH("Cash"),
    ONLINE("Online Payment"),
    UPI("UPI"),
    OTHER("Other");

    private final String displayName;
}