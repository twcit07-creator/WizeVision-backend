package com.thewizecompany.wizevision.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/*
 * Sent by Windows app when lock screen appears.
 *
 * idleStartActual:
 *   The corrected idle start time.
 *   Windows app calculates: detection_time - 3 minutes
 *   Example: App detected idle at 13:03:47
 *            Sends idleStartActual = 13:00:47
 *
 * We trust the Windows app to do this calculation
 * because it knows exactly when the last input was.
 * The backend stores exactly what the app sends.
 */
@Getter
@Setter
public class IdleStartRequest {

    /*
     * The actual time idle started
     * (last input time, not detection time).
     * Windows app sends this as: lastInputTime
     * from GetLastInputInfo Windows API.
     */
    @NotNull(message = "Idle start time is required")
    private Instant idleStartActual;

    /*
     * Current machine identifier.
     * Redundant with the header but
     * useful for offline sync payloads.
     */
    private String machineIdentifier;
}