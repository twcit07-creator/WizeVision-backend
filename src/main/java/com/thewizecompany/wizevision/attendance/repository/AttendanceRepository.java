package com.thewizecompany.wizevision.attendance.repository;

import com.thewizecompany.wizevision.attendance.domain.AttendanceRecord;
import com.thewizecompany.wizevision.attendance.domain.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository
        extends JpaRepository<AttendanceRecord, UUID> {

    /*
     * Primary lookup — today's record for an employee.
     * Called on every check-in/out event.
     */
    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
            UUID employeeId,
            LocalDate date
    );

    /*
     * Date range query — used for reports and payroll.
     */
    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetweenAndIsDeletedFalse(
            UUID employeeId,
            LocalDate from,
            LocalDate to
    );

    /*
     * All records for a specific date across all employees.
     * Used for daily attendance report.
     */
    List<AttendanceRecord> findByAttendanceDateAndIsDeletedFalse(
            LocalDate date
    );

    /*
     * Paginated history for an employee.
     * Used in employee profile → attendance tab.
     */
    Page<AttendanceRecord> findByEmployeeIdAndIsDeletedFalse(
            UUID employeeId,
            Pageable pageable
    );

    /*
     * Count present days in a month.
     * Used for payroll calculation:
     * Monthly salary ÷ working days × present days
     */
    @Query("""
        SELECT COUNT(r) FROM AttendanceRecord r
        WHERE r.employeeId = :employeeId
        AND r.attendanceDate BETWEEN :from AND :to
        AND r.status IN ('PRESENT', 'LATE', 'HALF_DAY')
        AND r.isDeleted = FALSE
        """)
    long countPresentDays(
            @Param("employeeId") UUID employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /*
     * Used for live dashboard — all employees
     * who have checked in today.
     */
    @Query("""
        SELECT r FROM AttendanceRecord r
        WHERE r.attendanceDate = :date
        AND r.checkInTime IS NOT NULL
        AND r.checkOutTime IS NULL
        AND r.isDeleted = FALSE
        """)
    List<AttendanceRecord> findCurrentlyCheckedIn(
            @Param("date") LocalDate date
    );
}