package com.cinx.reporting.reporting.api.dto;

import java.math.BigDecimal;

public record FinanceOverviewResponse(
        long paidOrders,
        BigDecimal paidRevenue
) {
}
