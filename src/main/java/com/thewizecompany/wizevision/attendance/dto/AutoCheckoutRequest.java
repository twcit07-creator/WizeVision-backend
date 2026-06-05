package com.thewizecompany.wizevision.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/*
 * Sent by Windows app after the 30-minute no-return threshold.
 *
 * TIMELINE:
 * 13:00  Idle detected (last input time)
 * 13:03  Lock screen shown
 * 13:33  30 minutes passed — this request is sent
 *
 * checkoutTime = 13:00 (the idle start time, NOT 13:33)
 * This is the fair checkout time — the last time
 * the employee was actually at their desk.
 *
 * The Windows app already knows idleStartActual
 * from when it sent IdleStartRequest.
 * It sends it again here for the checkout record.
 */
@Getter
@Setter
public class AutoCheckoutRequest {

    /*
     * The idle start actual time = auto checkout time.
     * Last time employee was confirmed at their desk.
     */
    @NotNull(message = "Checkout time is required")
    private Instant checkoutTime;

    private String machineIdentifier;
}