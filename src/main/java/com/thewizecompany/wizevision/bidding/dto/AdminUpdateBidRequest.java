package com.thewizecompany.wizevision.bidding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/*
 * Admin sets the bid amount and internal notes.
 * This is the ONLY way to set bid_amount.
 * PM cannot call this endpoint.
 */
@Getter
@Setter
public class AdminUpdateBidRequest {

    @NotNull(message = "Bid amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Bid amount must be greater than 0"
    )
    private BigDecimal bidAmount;

    private String internalNotes;
}