package com.thewizecompany.wizevision.bidding.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BidStatus {

    /*
     * DRAFT         → PM is working on it, not submitted yet
     * SUBMITTED     → PM submitted to admin for review
     * UNDER_REVIEW  → Admin is reviewing, entering amount
     * SENT_TO_CLIENT → Admin sent to client, awaiting response
     * NEGOTIATING   → Client wants to negotiate price
     * ACCEPTED      → Client accepted, project will be created
     * REJECTED      → Client rejected, bid closed
     * CANCELLED     → Cancelled internally before sending
     */
    DRAFT("Draft"),
    SUBMITTED("Submitted to Admin"),
    UNDER_REVIEW("Under Admin Review"),
    SENT_TO_CLIENT("Sent to Client"),
    NEGOTIATING("Negotiating"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String displayName;
}