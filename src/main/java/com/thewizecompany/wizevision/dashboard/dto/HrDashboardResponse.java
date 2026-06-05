package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class HrDashboardResponse {

    private final long totalHeadcount;
    private final long fullTimeCount;
    private final long contractCount;
    private final long freelanceCount;

    private final long pendingLeaveApplications;
    private final long employeesOnLeaveToday;
    private final long newJoineesThisMonth;

    private final BigDecimal currentMonthPayrollTotal;
    private final boolean payrollProcessedThisMonth;

    private final List<DepartmentHeadcountItem>
            headcountByDepartment;

    @Getter
    @Builder
    public static class DepartmentHeadcountItem {
        private final String department;
        private final long count;
    }
}