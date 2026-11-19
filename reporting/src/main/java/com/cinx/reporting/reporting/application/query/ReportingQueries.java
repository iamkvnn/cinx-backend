package com.cinx.reporting.reporting.application.query;

import java.math.BigDecimal;

/** Port for the figures reporting shows. Every number is read from the service's own projections of other services' events. */
public interface ReportingQueries {
    long countActiveAccounts();

    long countPaidOrders();

    /** Paid orders' totals after discounts; zero when nothing is paid. */
    BigDecimal paidRevenue();

    long countEnrollments();
}
