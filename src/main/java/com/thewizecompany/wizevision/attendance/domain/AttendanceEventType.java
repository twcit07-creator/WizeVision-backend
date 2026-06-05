package com.thewizecompany.wizevision.attendance.domain;

public enum AttendanceEventType {

    /*
     * CHECK_IN      — Employee starts work session
     * CHECK_OUT     — Employee ends work session
     * BREAK_START   — Employee starts a break
     * BREAK_END     — Employee returns from break
     * IDLE_START    — System detected no activity
     * IDLE_END      — Employee activity resumed
     * HEARTBEAT     — App is alive, employee still logged in
     */
    CHECK_IN,
    CHECK_OUT,
    BREAK_START,
    BREAK_END,
    IDLE_START,
    IDLE_END,
    HEARTBEAT
}