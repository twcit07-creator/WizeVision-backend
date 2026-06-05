package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ProjectReportResponse {

    private final long totalProjects;
    private final long activeProjects;
    private final long completedProjects;
    private final long cancelledProjects;
    private final BigDecimal totalContractValue;
    private final BigDecimal totalBilled;
    private final BigDecimal totalCollected;

    private final List<ProjectReportItem> projects;

    @Getter
    @Builder
    public static class ProjectReportItem {
        private final String projectNumber;
        private final String projectName;
        private final String clientName;
        private final String pmName;
        private final String status;
        private final String currentPhase;
        private final int progressPercentage;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final BigDecimal contractAmount;
        private final BigDecimal changeOrdersTotal;
        private final BigDecimal totalInvoiced;
        private final BigDecimal totalPaid;
        private final BigDecimal outstanding;
        private final long daysRunning;
    }
}