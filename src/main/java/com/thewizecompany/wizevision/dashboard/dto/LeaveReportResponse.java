package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LeaveReportResponse {

    private final int year;
    private final long totalApplications;
    private final long approvedApplications;
    private final long rejectedApplications;
    private final long pendingApplications;

    private final List<EmployeeLeaveItem> byEmployee;
    private final List<LeaveTypeItem> byLeaveType;

    @Getter
    @Builder
    public static class EmployeeLeaveItem {
        private final String employeeCode;
        private final String employeeName;
        private final String department;
        private final double annualUsed;
        private final double sickUsed;
        private final double casualUsed;
        private final double unpaidUsed;
        private final double totalUsed;
    }

    @Getter
    @Builder
    public static class LeaveTypeItem {
        private final String leaveType;
        private final double totalDaysTaken;
        private final long applicationCount;
    }
}