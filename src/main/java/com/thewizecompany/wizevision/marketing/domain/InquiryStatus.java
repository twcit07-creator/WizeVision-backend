package com.thewizecompany.wizevision.marketing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {

    /*
     * NEW             → Created by marketing
     * FORWARDED       → Sent to PM for bidding
     * BID_IN_PROGRESS → PM is working on the bid
     * BID_CREATED     → Bid has been submitted to admin
     * CLOSED          → Project won or lost
     */
    NEW("New"),
    FORWARDED("Forwarded to PM"),
    BID_IN_PROGRESS("Bid In Progress"),
    BID_CREATED("Bid Created"),
    CLOSED("Closed");

    private final String displayName;
}