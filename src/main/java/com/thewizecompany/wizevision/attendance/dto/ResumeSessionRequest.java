package com.thewizecompany.wizevision.attendance.dto;

import com.thewizecompany.wizevision.attendance.domain.IdleReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/*
 * Sent when employee re-logs in after auto-checkout.
 *
 * This is different from IdleEndRequest because:
 * - IdleEndRequest = returned within 30 mins (session never ended)
 * - ResumeSessionRequest = re-login after session auto-ended
 *
 * Windows app shows after re-login:
 * ┌──────────────────────────────────────────┐
 * │  Welcome back, John!                     │
 * │                                          │
 * │  You were away for 1 hour 23 minutes     │
 * │  (13:00 - 14:23)                         │
 * │                                          │
 * │  Please select reason for absence:       │
 * │  ○ Lunch Break                           │
 * │  ○ Tea Break                             │
 * │  ○ In a Meeting                          │
 * │  ○ Project Discussion                    │
 * │  ○ Team Guidance                         │
 * │  ○ Other                                 │
 * │                                          │
 * │              [Resume Work]               │
 * └──────────────────────────────────────────┘
 */
@Getter
@Setter
public class ResumeSessionRequest {

    @NotNull(message = "Resume time is required")
    private Instant resumeTime;

    @NotNull(message = "Please select a reason for your absence")
    private IdleReason absenceReason;

    private String machineIdentifier;
    private String machineName;
}