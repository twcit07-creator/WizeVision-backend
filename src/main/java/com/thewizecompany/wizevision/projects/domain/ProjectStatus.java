package com.thewizecompany.wizevision.projects.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectStatus {

    /*
     * PLANNING    → Project created from accepted bid,
     *               PM is setting up team and timeline
     * ACTIVE      → Work has started
     * ON_HOLD     → Temporarily paused (client request,
     *               missing drawings, etc.)
     * DELIVERED   → Work sent to client for review
     * COMPLETED   → Client approved, project closed
     * CANCELLED   → Project cancelled after starting
     */
    PLANNING("Planning"),
    ACTIVE("Active"),
    ON_HOLD("On Hold"),
    DELIVERED("Delivered to Client"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;
}