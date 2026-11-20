package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.domain.StatisticsGroupBy;

import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.application.query.AdminCourseStatisticsOverviewView;
import com.cinx.learning.catalog.application.query.InstructorCourseStatisticsOverviewView;
import com.cinx.learning.catalog.application.query.StatisticsByTimeView;
import com.cinx.learning.catalog.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseStatisticsService implements ICourseStatisticsService {
    private final CourseRepository courseRepository;
    private final StatisticsRangeResolver statisticsRangeResolver = new StatisticsRangeResolver();

    @Override
    @Transactional(readOnly = true)
    public AdminCourseStatisticsOverviewView getAdminOverview(StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        StatisticsDateRange range = statisticsRangeResolver.resolve(groupBy, startDate, endDate);
        List<Object[]> timeRows = range.groupByDay()
                ? courseRepository.aggregateCreatedCoursesByDay(range.start(), range.end())
                : courseRepository.aggregateCreatedCoursesByMonth(range.start(), range.end());
        return new AdminCourseStatisticsOverviewView(
                courseRepository.countCreatedCoursesBetween(range.start(), range.end()),
                toStringLongMap(courseRepository.countCreatedCoursesByStatusBetween(range.start(), range.end())),
                fillByTime(range, timeRows),
                toStringLongMap(courseRepository.countCurrentCoursesByStatus()),
                courseRepository.countByStatus(CourseStatus.PUBLISHED)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorCourseStatisticsOverviewView getInstructorOverview(String instructorId, StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        StatisticsDateRange range = statisticsRangeResolver.resolve(groupBy, startDate, endDate);
        List<Object[]> timeRows = range.groupByDay()
                ? courseRepository.aggregateCreatedCoursesByInstructorAndDay(instructorId, range.start(), range.end())
                : courseRepository.aggregateCreatedCoursesByInstructorAndMonth(instructorId, range.start(), range.end());
        Double averageRating = courseRepository.averageRatingByInstructorId(instructorId);
        Long enrollmentSnapshot = courseRepository.sumEnrollmentCountByInstructorId(instructorId);
        return new InstructorCourseStatisticsOverviewView(
                courseRepository.countCreatedCoursesByInstructorBetween(instructorId, range.start(), range.end()),
                fillByTime(range, timeRows),
                courseRepository.countByInstructorId(instructorId),
                courseRepository.countByInstructorIdAndStatus(instructorId, CourseStatus.PUBLISHED),
                averageRating,
                enrollmentSnapshot != null ? enrollmentSnapshot : 0L
        );
    }

    private List<StatisticsByTimeView> fillByTime(StatisticsDateRange range, List<Object[]> rows) {
        Map<String, Long> valuesByLabel = new LinkedHashMap<>();
        range.bucketLabels().forEach(label -> valuesByLabel.put(label, 0L));
        rows.forEach(row -> valuesByLabel.put((String) row[0], ((Number) row[1]).longValue()));
        return valuesByLabel.entrySet().stream()
                .map(entry -> new StatisticsByTimeView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, Long> toStringLongMap(List<Object[]> rows) {
        Map<String, Long> values = new LinkedHashMap<>();
        rows.forEach(row -> values.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        return values;
    }
}

