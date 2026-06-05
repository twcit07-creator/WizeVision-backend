package com.thewizecompany.wizevision.attendance.dto;

import com.thewizecompany.wizevision.attendance.domain.AttendanceEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/*
 * Sent by Windows app for each attendance event.
 * Used for real-time events (not offline sync).
 */
@Getter
@Setter
public class AttendanceEventRequest {

    @NotNull(message = "Event type is required")
    private AttendanceEventType eventType;

    /*
     * If null, server uses current time.
     * Windows app should always send this for accuracy.
     */
    private Instant eventTime;

    private String notes;
}