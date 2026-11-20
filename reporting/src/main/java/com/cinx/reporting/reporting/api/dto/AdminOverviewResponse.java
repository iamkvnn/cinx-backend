package com.cinx.reporting.reporting.api.dto;

import java.math.BigDecimal;

public record AdminOverviewResponse(
        long activeAccounts,
        long paidOrders,
        BigDecimal paidRevenue,
        long enrollments
) {
}
