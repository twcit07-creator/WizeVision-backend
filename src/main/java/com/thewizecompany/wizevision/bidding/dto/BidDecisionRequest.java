package com.thewizecompany.wizevision.bidding.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/*
 * Admin records client's decision.
 * Used for both ACCEPT and REJECT.
 */
@Getter
@Setter
public class BidDecisionRequest {

    @NotNull(message = "Decision is required")
    private BidDecision decision;

    /*
     * Required when decision = REJECTED.
     * Optional for ACCEPTED.
     */
    private String reason;

    public enum BidDecision {
        ACCEPTED,
        REJECTED,
        NEGOTIATING
    }
}