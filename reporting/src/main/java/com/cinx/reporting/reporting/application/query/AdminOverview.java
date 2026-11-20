package com.cinx.reporting.reporting.application.query;

import java.math.BigDecimal;

public record AdminOverview(long activeAccounts, long paidOrders, BigDecimal paidRevenue, long enrollments) {
}
