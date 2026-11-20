package com.cinx.reporting.reporting.application.query;

import java.math.BigDecimal;

public record FinanceOverview(long paidOrders, BigDecimal paidRevenue) {
}
