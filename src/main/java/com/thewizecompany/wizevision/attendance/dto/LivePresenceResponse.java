package com.thewizecompany.wizevision.attendance.dto;

import com.thewizecompany.wizevision.employee.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/*
 * Used by the live presence dashboard in the web portal.
 * Admin/PM/TL can see all employees and their current status.
 *
 * Updated in real-time as events come in.
 * Cached in Redis with short TTL for fast reads.
 */
@Getter
@Builder
public class LivePresenceResponse {

    private final UUID employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String department;
    private final Role role;

    private final String machineIdentifier;
    private final String machineName;
    private final boolean online;

    /*
     * Current status:
     * WORKING / ON_BREAK / IDLE / CHECKED_OUT / NOT_CHECKED_IN
     */
    private final String currentStatus;

    private final Instant checkInTime;
    private final Integer totalWorkMinutes;
    private final Integer totalBreakMinutes;
    private final Integer totalIdleMinutes;
    private final Instant lastHeartbeat;
}