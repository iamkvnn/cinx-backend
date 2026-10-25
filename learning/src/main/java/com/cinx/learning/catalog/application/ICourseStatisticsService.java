package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.domain.StatisticsGroupBy;

import com.cinx.learning.catalog.application.query.AdminCourseStatisticsOverviewView;
import com.cinx.learning.catalog.application.query.InstructorCourseStatisticsOverviewView;

import java.time.LocalDate;

public interface ICourseStatisticsService {
    AdminCourseStatisticsOverviewView getAdminOverview(StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate);

    InstructorCourseStatisticsOverviewView getInstructorOverview(String instructorId, StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate);
}

