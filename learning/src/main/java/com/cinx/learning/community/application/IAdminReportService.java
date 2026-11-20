package com.cinx.learning.community.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.community.application.query.AdminReportView;
import com.cinx.learning.community.domain.ReportType;

public interface IAdminReportService {
    PageResult<AdminReportView> getReports(ReportType type, int page, int size, String query, String sort);
    void dismissReport(String reportId);
    void deleteReportedContent(String reportId);
}
