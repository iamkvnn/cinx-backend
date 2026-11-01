package com.cinx.learning.community.application;

import com.cinx.common.paging.PageQuery;

import com.cinx.learning.community.domain.StatisticsGroupBy;

import com.cinx.learning.community.application.query.CourseQnAStatisticsView;
import com.cinx.learning.community.application.query.ReportStatisticsOverviewView;
import com.cinx.learning.community.application.query.ReviewStatisticsView;
import com.cinx.learning.community.application.query.StatisticsByTimeView;
import com.cinx.learning.community.application.query.TopReportedRefView;
import com.cinx.learning.community.domain.CourseAnswerRepository;
import com.cinx.learning.community.domain.CourseQuestionRepository;
import com.cinx.learning.community.domain.ReportRepository;
import com.cinx.learning.community.domain.ReviewReplyRepository;
import com.cinx.learning.community.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialStatisticsService implements ISocialStatisticsService {
    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final CourseQuestionRepository courseQuestionRepository;
    private final CourseAnswerRepository courseAnswerRepository;
    private final ReportRepository reportRepository;
    private final StatisticsRangeResolver statisticsRangeResolver = new StatisticsRangeResolver();

    @Override
    @Transactional(readOnly = true)
    public ReviewStatisticsView getReviewStatistics(String courseId) {
        Long reviewCount = reviewRepository.countByCourseId(courseId);
        Long replyCount = reviewReplyRepository.countRepliesByCourseId(courseId);
        Double averageRating = reviewRepository.getAverageRatingByCourseId(courseId);
        return new ReviewStatisticsView(
                reviewCount,
                averageRating,
                ratingDistribution(reviewRepository.countRatingsByCourseId(courseId)),
                reviewCount > 0 ? replyCount * 100.0 / reviewCount : 0.0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CourseQnAStatisticsView getCourseQnAStatistics(String courseId, StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        StatisticsDateRange range = statisticsRangeResolver.resolve(groupBy, startDate, endDate);
        Long questionsInRange = courseQuestionRepository.countQuestionsByCourseIdBetween(courseId, range.start(), range.end());
        Long instructorAnsweredQuestions = courseQuestionRepository.countQuestionsWithInstructorAnswerBetween(courseId, range.start(), range.end());
        List<Object[]> questionRows = range.groupByDay()
                ? courseQuestionRepository.aggregateQuestionsByDay(courseId, range.start(), range.end())
                : courseQuestionRepository.aggregateQuestionsByMonth(courseId, range.start(), range.end());
        return new CourseQnAStatisticsView(
                questionsInRange,
                courseAnswerRepository.countAnswersByCourseIdBetween(courseId, range.start(), range.end()),
                questionsInRange - instructorAnsweredQuestions,
                questionsInRange > 0 ? instructorAnsweredQuestions * 100.0 / questionsInRange : 0.0,
                fillByTime(range, questionRows)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReportStatisticsOverviewView getReportOverview(StatisticsGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        StatisticsDateRange range = statisticsRangeResolver.resolve(groupBy, startDate, endDate);
        List<Object[]> reportRows = range.groupByDay()
                ? reportRepository.aggregateReportsByDay(range.start(), range.end())
                : reportRepository.aggregateReportsByMonth(range.start(), range.end());
        return new ReportStatisticsOverviewView(
                reportRepository.countReportsBetween(range.start(), range.end()),
                toStringLongMap(reportRepository.countReportsByTypeBetween(range.start(), range.end())),
                fillByTime(range, reportRows),
                toTopReportedRefs(reportRepository.findTopReportedRefs(range.start(), range.end(), PageQuery.first(5)))
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

    /** "1".."5", always all five: ratings are stored as Double, so String.valueOf gave "5.0" and left gaps. */
    private Map<String, Long> ratingDistribution(List<Object[]> rows) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            values.put(String.valueOf(star), 0L);
        }
        rows.forEach(row -> {
            if (row[0] instanceof Number rating) {
                long star = Math.max(1, Math.min(5, Math.round(rating.doubleValue())));
                values.merge(String.valueOf(star), ((Number) row[1]).longValue(), Long::sum);
            }
        });
        return values;
    }

    private Map<String, Long> toStringLongMap(List<Object[]> rows) {
        Map<String, Long> values = new LinkedHashMap<>();
        rows.forEach(row -> values.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        return values;
    }

    private List<TopReportedRefView> toTopReportedRefs(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new TopReportedRefView(
                        (String) row[0],
                        (com.cinx.learning.community.domain.ReportType) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }
}
