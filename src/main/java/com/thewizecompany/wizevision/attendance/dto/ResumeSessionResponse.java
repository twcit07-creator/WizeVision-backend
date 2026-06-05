package com.thewizecompany.wizevision.attendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/*
 * Response to ResumeSessionRequest.
 *
 * Windows app uses this to decide what to show next:
 *
 * If canResume = true:
 *   Show "Welcome back! Session resumed."
 *   Continue normal work tracking.
 *
 * If canResume = false (dayEnded = true):
 *   Show "Your work day has ended."
 *   "Your checkout was recorded at {autoCheckoutTime}"
 *   "Total work today: X hours Y minutes"
 *   App returns to login screen.
 *   No further work tracking for today.
 */
@Getter
@Builder
public class ResumeSessionResponse {

    /*
     * True = session can continue (gap < 4 hours)
     * False = day has ended (gap >= 4 hours)
     */
    private final boolean canResume;

    /*
     * Only relevant when canResume = false.
     * The time the auto-checkout was recorded.
     * Shown to employee: "Your checkout was at 14:05"
     */
    private final Instant autoCheckoutTime;

    /*
     * Shown on the resume confirmation screen:
     * "You were away for 1 hour 23 minutes"
     */
    private final int absenceDurationMinutes;

    private final Instant absenceStartTime;
    private final Instant absenceEndTime;

    /*
     * Summary shown when day ends:
     * "Total work today: 6 hours 45 minutes"
     */
    private final Integer totalWorkMinutesToday;

    /*
     * Message for the Windows app UI.
     * canResume = true:  "Session resumed successfully"
     * canResume = false: "Your work day has ended.
     *                    See you tomorrow!"
     */
    private final String message;
}