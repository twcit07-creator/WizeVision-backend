package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AttendanceReportResponse {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final int totalWorkingDays;
    private final List<EmployeeAttendanceSummary> employees;

    @Getter
    @Builder
    public static class EmployeeAttendanceSummary {

        private final UUID employeeId;
        private final String employeeCode;
        private final String employeeName;
        private final String department;

        private final int presentDays;
        private final int absentDays;
        private final int lateDays;
        private final int leaveDays;
        private final int halfDays;

        private final double totalWorkHours;
        private final double totalIdleHours;
        private final double totalOvertimeHours;

        private final double attendancePercentage;
    }
}