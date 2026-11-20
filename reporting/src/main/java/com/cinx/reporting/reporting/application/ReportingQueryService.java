package com.cinx.reporting.reporting.application;

import com.cinx.reporting.reporting.application.query.AdminOverview;
import com.cinx.reporting.reporting.application.query.DashboardOverview;
import com.cinx.reporting.reporting.application.query.FinanceOverview;
import com.cinx.reporting.reporting.application.query.ReportingQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportingQueryService {
    private final ReportingQueries queries;

    @Transactional(readOnly = true)
    public AdminOverview adminOverview() {
        return new AdminOverview(
                queries.countActiveAccounts(),
                queries.countPaidOrders(),
                queries.paidRevenue(),
                queries.countEnrollments());
    }

    @Transactional(readOnly = true)
    public DashboardOverview dashboardOverview() {
        return new DashboardOverview(
                queries.countActiveAccounts(),
                queries.countPaidOrders(),
                queries.countEnrollments());
    }

    @Transactional(readOnly = true)
    public FinanceOverview financeOverview() {
        return new FinanceOverview(
                queries.countPaidOrders(),
                queries.paidRevenue());
    }
}
