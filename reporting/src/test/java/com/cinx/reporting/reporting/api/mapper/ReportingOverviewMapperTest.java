package com.cinx.reporting.reporting.api.mapper;

import com.cinx.reporting.reporting.application.query.AdminOverview;
import com.cinx.reporting.reporting.application.query.DashboardOverview;
import com.cinx.reporting.reporting.application.query.FinanceOverview;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReportingOverviewMapperTest {
    private final ReportingOverviewMapper mapper = Mappers.getMapper(ReportingOverviewMapper.class);

    @Test
    void theDashboardResponseKeepsItsThreeFigures() {
        var response = mapper.toDto(new DashboardOverview(120, 45, 310));

        assertThat(response.activeAccounts()).isEqualTo(120);
        assertThat(response.paidOrders()).isEqualTo(45);
        assertThat(response.enrollments()).isEqualTo(310);
    }

    @Test
    void theFinanceResponseKeepsTheRevenueToTheCent() {
        var response = mapper.toDto(new FinanceOverview(45, new BigDecimal("12500000.50")));

        assertThat(response.paidOrders()).isEqualTo(45);
        assertThat(response.paidRevenue()).isEqualByComparingTo("12500000.50");
    }

    @Test
    void theAdminResponseKeepsAllFourFigures() {
        var response = mapper.toDto(new AdminOverview(120, 45, new BigDecimal("1.00"), 310));

        assertThat(response.activeAccounts()).isEqualTo(120);
        assertThat(response.paidRevenue()).isEqualByComparingTo("1.00");
        assertThat(response.enrollments()).isEqualTo(310);
    }
}
