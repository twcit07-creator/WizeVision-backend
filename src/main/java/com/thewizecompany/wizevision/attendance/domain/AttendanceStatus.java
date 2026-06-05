package com.thewizecompany.wizevision.attendance.domain;

public enum AttendanceStatus {

    /*
     * PRESENT        — Employee checked in, working normally
     * ABSENT         — No check-in recorded for the day
     * HALF_DAY       — Worked less than half the shift
     * ON_LEAVE       — Approved leave for the day
     * HOLIDAY        — Public holiday
     * LATE           — Checked in after grace period
     * WORK_FROM_HOME — Future feature placeholder
     */
    PRESENT,
    ABSENT,
    HALF_DAY,
    ON_LEAVE,
    HOLIDAY,
    LATE,
    WORK_FROM_HOME
}