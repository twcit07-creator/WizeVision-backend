package com.thewizecompany.wizevision.attendance.dto;

import com.thewizecompany.wizevision.attendance.domain.IdleReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/*
 * Sent by Windows app when employee returns
 * within the 30-minute window and selects a reason.
 *
 * Windows app shows:
 * ┌──────────────────────────────────────────┐
 * │  You were away for 23 minutes            │
 * │  (13:00 - 13:23)                         │
 * │                                          │
 * │  Please select a reason:                 │
 * │  ○ Lunch Break                           │
 * │  ○ Tea Break                             │
 * │  ○ Washroom Break                        │
 * │  ○ In a Meeting                          │
 * │  ○ Project Discussion                    │
 * │  ○ Team Guidance                         │
 * │  ○ Other                                 │
 * │                                          │
 * │              [Continue Working]          │
 * └──────────────────────────────────────────┘
 *
 * Employee selects reason → app sends this request.
 */
@Getter
@Setter
public class IdleEndRequest {

    @NotNull(message = "Return time is required")
    private Instant returnTime;

    @NotNull(message = "Please select a reason for your absence")
    private IdleReason reason;

    private String machineIdentifier;
}