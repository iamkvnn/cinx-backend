package com.cinx.reporting.reporting.api.mapper;

import com.cinx.reporting.reporting.api.dto.AdminOverviewResponse;
import com.cinx.reporting.reporting.api.dto.FinanceOverviewResponse;
import com.cinx.reporting.reporting.api.dto.ReportingOverviewResponse;
import com.cinx.reporting.reporting.application.query.AdminOverview;
import com.cinx.reporting.reporting.application.query.DashboardOverview;
import com.cinx.reporting.reporting.application.query.FinanceOverview;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReportingOverviewMapper {
    AdminOverviewResponse toDto(AdminOverview overview);

    ReportingOverviewResponse toDto(DashboardOverview overview);

    FinanceOverviewResponse toDto(FinanceOverview overview);
}
