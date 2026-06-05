package com.thewizecompany.wizevision.hr.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveStatus {

    /*
     * PENDING      → Just submitted by employee
     * TL_APPROVED  → Team lead approved, waiting for PM
     * PM_APPROVED  → PM approved, waiting for HR confirmation
     * APPROVED     → Fully approved (HR confirmed or no TL/PM)
     * REJECTED     → Rejected at any stage
     * CANCELLED    → Cancelled by employee before approval
     */
    PENDING("Pending Approval"),
    TL_APPROVED("Approved by Team Lead"),
    PM_APPROVED("Approved by Project Manager"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String displayName;
}