package com.thewizecompany.wizevision.attendance.repository;

import com.thewizecompany.wizevision.attendance.domain.AttendanceEvent;
import com.thewizecompany.wizevision.attendance.domain.AttendanceEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceEventRepository
        extends JpaRepository<AttendanceEvent, UUID> {

    /*
     * All events for a record — used to show
     * the detailed timeline of a day.
     * Ordered by event_time ASC for chronological display.
     */
    List<AttendanceEvent> findByAttendanceRecordIdAndIsDeletedFalseOrderByEventTimeAsc(
            UUID attendanceRecordId
    );

    /*
     * Most recent event for an employee today.
     * Used to determine current state:
     * Last event = CHECK_IN → employee is working
     * Last event = BREAK_START → employee is on break
     */
    @Query("""
        SELECT e FROM AttendanceEvent e
        WHERE e.employeeId = :employeeId
        AND e.eventTime >= :startOfDay
        AND e.isDeleted = FALSE
        ORDER BY e.eventTime DESC
        LIMIT 1
        """)
    Optional<AttendanceEvent> findLastEventToday(
            @Param("employeeId") UUID employeeId,
            @Param("startOfDay") Instant startOfDay
    );

    /*
     * Find the last event of a specific type.
     * Used to calculate durations:
     * Find last BREAK_START → BREAK_END - BREAK_START = break duration
     */
    @Query("""
        SELECT e FROM AttendanceEvent e
        WHERE e.employeeId = :employeeId
        AND e.eventType = :eventType
        AND e.eventTime >= :startOfDay
        AND e.isDeleted = FALSE
        ORDER BY e.eventTime DESC
        LIMIT 1
        """)
    Optional<AttendanceEvent> findLastEventOfTypeToday(
            @Param("employeeId") UUID employeeId,
            @Param("eventType") AttendanceEventType eventType,
            @Param("startOfDay") Instant startOfDay
    );

    /*
     * All events for an employee in a time range.
     * Used for detailed idle reports.
     */
    List<AttendanceEvent> findByEmployeeIdAndEventTimeBetweenAndIsDeletedFalseOrderByEventTimeAsc(
            UUID employeeId,
            Instant from,
            Instant to
    );
}