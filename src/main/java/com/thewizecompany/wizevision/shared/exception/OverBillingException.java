package com.thewizecompany.wizevision.shared.exception;

/*
 * Thrown when an invoice would exceed the
 * target amount (contract or change order).
 * Returns HTTP 422 with clear message.
 */
public class OverBillingException extends RuntimeException {

    public OverBillingException(String message) {
        super(message);
    }
}