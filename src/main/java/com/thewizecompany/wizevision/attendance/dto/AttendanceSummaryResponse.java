package com.thewizecompany.wizevision.attendance.dto;

import com.thewizecompany.wizevision.attendance.domain.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class AttendanceSummaryResponse {

    private final UUID id;
    private final UUID employeeId;
    private final LocalDate attendanceDate;
    private final AttendanceStatus status;

    private final Instant checkInTime;
    private final Instant checkOutTime;
    private final Integer totalWorkMinutes;
    private final Integer totalBreakMinutes;
    private final Integer totalIdleMinutes;
    private final Integer overtimeMinutes;

    private final String machineIdentifier;
    private final String machineName;

    private final boolean late;
    private final Integer lateByMinutes;
    private final boolean manuallyAdjusted;
    private final String adjustmentNote;
}