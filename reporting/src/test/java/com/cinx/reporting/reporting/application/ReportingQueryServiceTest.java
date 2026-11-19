package com.cinx.reporting.reporting.application;

import com.cinx.reporting.reporting.application.query.AdminOverview;
import com.cinx.reporting.reporting.application.query.DashboardOverview;
import com.cinx.reporting.reporting.application.query.FinanceOverview;
import com.cinx.reporting.reporting.application.query.ReportingQueries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingQueryServiceTest {
    @Mock
    private ReportingQueries queries;
    private ReportingQueryService service;

    @BeforeEach
    void setUp() {
        service = new ReportingQueryService(queries);
    }

    @Test
    void theAdminOverviewCarriesAllFourFigures() {
        when(queries.countActiveAccounts()).thenReturn(120L);
        when(queries.countPaidOrders()).thenReturn(45L);
        when(queries.paidRevenue()).thenReturn(new BigDecimal("12500000.50"));
        when(queries.countEnrollments()).thenReturn(310L);

        assertThat(service.adminOverview()).isEqualTo(new AdminOverview(120, 45, new BigDecimal("12500000.50"), 310));
    }

    @Test
    void theDashboardOverviewHasNoRevenueAndNeverAsksForIt() {
        when(queries.countActiveAccounts()).thenReturn(120L);
        when(queries.countPaidOrders()).thenReturn(45L);
        when(queries.countEnrollments()).thenReturn(310L);

        assertThat(service.dashboardOverview()).isEqualTo(new DashboardOverview(120, 45, 310));
        verify(queries).countActiveAccounts();
        verify(queries).countPaidOrders();
        verify(queries).countEnrollments();
        verifyNoMoreInteractions(queries);
    }

    @Test
    void theFinanceOverviewIsPaidOrdersAndRevenueOnly() {
        when(queries.countPaidOrders()).thenReturn(45L);
        when(queries.paidRevenue()).thenReturn(new BigDecimal("12500000.50"));

        assertThat(service.financeOverview()).isEqualTo(new FinanceOverview(45, new BigDecimal("12500000.50")));
        verifyNoMoreInteractions(queries);
    }
}
