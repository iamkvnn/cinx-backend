package com.cinx.reporting.reporting.api.dto;

public record ReportingOverviewResponse(
        long activeAccounts,
        long paidOrders,
        long enrollments
) {
}
