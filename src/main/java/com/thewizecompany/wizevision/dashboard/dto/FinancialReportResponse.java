package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FinancialReportResponse {

    private final int year;
    private final BigDecimal totalRevenue;
    private final BigDecimal totalCollected;
    private final BigDecimal totalOutstanding;

    private final List<MonthlyRevenuItem> monthlyBreakdown;
    private final List<ClientRevenueItem> byClient;

    @Getter
    @Builder
    public static class MonthlyRevenuItem {
        private final String month;
        private final int monthNumber;
        private final BigDecimal invoiced;
        private final BigDecimal collected;
        private final BigDecimal outstanding;
        private final long invoiceCount;
    }

    @Getter
    @Builder
    public static class ClientRevenueItem {
        private final String clientName;
        private final String clientCode;
        private final long projectCount;
        private final BigDecimal totalBilled;
        private final BigDecimal totalCollected;
        private final BigDecimal outstanding;
    }
}