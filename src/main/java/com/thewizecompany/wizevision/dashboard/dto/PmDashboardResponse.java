package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PmDashboardResponse {

    private final long myActiveProjects;
    private final long myProjectsInPlanning;
    private final long myProjectsOnHold;
    private final long myDraftBids;
    private final long mySubmittedBids;
    private final long inquiriesWaitingForBid;
    private final long leaveApprovalsNeeded;

    /*
     * Quick list for the dashboard page.
     * Full details available via /projects/my endpoint.
     */
    private final List<ProjectSummaryItem> activeProjects;
    private final List<InquirySummaryItem> pendingInquiries;

    @Getter
    @Builder
    public static class ProjectSummaryItem {
        private final String projectNumber;
        private final String projectName;
        private final String clientName;
        private final String status;
        private final String phase;
        private final int progressPercentage;
        private final boolean teamAssigned;
    }

    @Getter
    @Builder
    public static class InquirySummaryItem {
        private final String inquiryNumber;
        private final String projectName;
        private final String clientName;
        private final String forwardedAt;
    }
}