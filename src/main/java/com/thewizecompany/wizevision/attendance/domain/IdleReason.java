package com.thewizecompany.wizevision.attendance.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * IDLE / ABSENCE REASONS
 *
 * Hardcoded list — shown in Windows app when:
 * 1. User returns within 30 mins (idle end)
 * 2. User re-logs in after auto-checkout (absence reason)
 *
 * displayName is what the Windows app shows
 * in the dropdown UI.
 */
@Getter
@RequiredArgsConstructor
public enum IdleReason {

    LUNCH_BREAK("Lunch Break"),
    TEA_BREAK("Tea Break"),
    WASHROOM_BREAK("Washroom Break"),
    IN_MEETING("In a Meeting"),
    PROJECT_DISCUSSION("Project Discussion"),
    TEAM_GUIDANCE("Team Guidance"),
    OTHER("Other");

    private final String displayName;
}