package com.thewizecompany.wizevision.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FinanceDashboardResponse {

    private final BigDecimal totalInvoicedAllTime;
    private final BigDecimal totalCollectedAllTime;
    private final BigDecimal totalOutstanding;

    private final BigDecimal invoicedThisMonth;
    private final BigDecimal collectedThisMonth;

    private final long overdueCount;
    private final BigDecimal overdueAmount;

    private final long draftInvoicesCount;
    private final long sentInvoicesCount;
    private final long partiallyPaidCount;

    /*
     * Invoice aging buckets.
     * How long invoices have been outstanding.
     */
    private final BigDecimal aging0to30;
    private final BigDecimal aging31to60;
    private final BigDecimal aging61to90;
    private final BigDecimal agingOver90;

    private final List<TopOutstandingClientItem>
            topOutstandingClients;

    @Getter
    @Builder
    public static class TopOutstandingClientItem {
        private final String clientName;
        private final String clientCode;
        private final BigDecimal outstandingAmount;
        private final long overdueInvoices;
    }
}