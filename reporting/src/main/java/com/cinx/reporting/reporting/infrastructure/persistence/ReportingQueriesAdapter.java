package com.cinx.reporting.reporting.infrastructure.persistence;

import com.cinx.reporting.reporting.application.query.ReportingQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
class ReportingQueriesAdapter implements ReportingQueries {
    private final AccountProjectionRepository accounts;
    private final OrderProjectionRepository orders;
    private final EnrollmentProjectionRepository enrollments;

    @Override
    public long countActiveAccounts() {
        return accounts.countByStatus("ACTIVE");
    }

    @Override
    public long countPaidOrders() {
        return orders.countByStatus("PAID");
    }

    @Override
    public BigDecimal paidRevenue() {
        BigDecimal revenue = orders.paidRevenue();
        return revenue == null ? BigDecimal.ZERO : revenue;
    }

    @Override
    public long countEnrollments() {
        return enrollments.count();
    }
}
