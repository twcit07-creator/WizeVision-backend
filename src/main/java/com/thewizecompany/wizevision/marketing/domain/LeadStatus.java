package com.thewizecompany.wizevision.marketing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeadStatus {

    /*
     * NEW         → Just added, not yet contacted
     * CONTACTED   → Marketing reached out
     * QUALIFIED   → Has a real project, worth pursuing
     * CONVERTED   → Became a client, project inquiry created
     * LOST        → Not interested or went to competitor
     */
    NEW("New"),
    CONTACTED("Contacted"),
    QUALIFIED("Qualified"),
    CONVERTED("Converted"),
    LOST("Lost");

    private final String displayName;
}