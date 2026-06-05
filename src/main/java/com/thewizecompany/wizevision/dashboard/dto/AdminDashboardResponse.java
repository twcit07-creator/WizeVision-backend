package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AdminDashboardResponse {

    // ── EMPLOYEE SUMMARY ──────────────────────────────────────
    private final long totalEmployees;
    private final long activeEmployees;
    private final long employeesOnLeaveToday;
    private final long newJoineesThisMonth;

    // ── PROJECT SUMMARY ───────────────────────────────────────
    private final long totalActiveProjects;
    private final long projectsInPlanning;
    private final long projectsOnHold;
    private final long projectsDeliveredThisMonth;

    // ── BIDDING SUMMARY ───────────────────────────────────────
    private final long bidsSubmittedPendingReview;
    private final long bidsNegotiating;
    private final long bidsAcceptedThisMonth;
    private final long bidsRejectedThisMonth;

    // ── FINANCIAL SUMMARY ─────────────────────────────────────
    private final BigDecimal totalOutstandingAmount;
    private final BigDecimal revenueThisMonth;
    private final BigDecimal revenueLastMonth;
    private final long overdueInvoicesCount;
    private final BigDecimal overdueInvoicesAmount;

    // ── HR SUMMARY ────────────────────────────────────────────
    private final long leaveApplicationsPendingHr;

    // ── RECENT ACTIVITY ───────────────────────────────────────
    private final List<RecentActivityItem> recentActivity;

    @Getter
    @Builder
    public static class RecentActivityItem {
        private final String type;
        private final String description;
        private final String referenceNumber;
        private final String timestamp;
    }
}