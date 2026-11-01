package com.cinx.learning.engagement.application;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.learning.engagement.domain.LearningActivityGroupBy;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.engagement.application.command.LearningActivityCommand;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.progress.application.query.CourseProgressSummaryView;
import com.cinx.learning.progress.application.query.CoursesProgressSummaryView;
import com.cinx.learning.engagement.application.query.LearningActivityByTimeView;
import com.cinx.learning.engagement.application.query.LearningActivitySummaryView;
import com.cinx.learning.engagement.application.query.UserLearningSummaryView;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.engagement.domain.LearningActivityDaily;
import com.cinx.learning.engagement.domain.LearningActivityDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningActivityService implements ILearningActivityService {
    private static final int MAX_HEARTBEAT_SECONDS = 300;
    private final ProgressLookup progressLookup;
    private final LearningActivityDailyRepository learningActivityDailyRepository;
    private final Clock clock;
    private final LearningActivityRangeResolver activityRangeResolver = new LearningActivityRangeResolver();

    @Override
    @Transactional
    public void recordActivity(String userId, LearningActivityCommand request) {
        recordActivity(userId, request.courseId(), request.activeSeconds(), request.itemId());
    }

    @Override
    @Transactional
    public void recordActivity(String userId, String courseId, Integer activeSeconds) {
        recordActivity(userId, courseId, activeSeconds, null);
    }

    private void recordActivity(String userId, String courseId, Integer activeSeconds, String itemId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "userId must not be blank");
        }
        if (courseId == null || courseId.isBlank()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "courseId must not be blank");
        }
        if (activeSeconds == null) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "activeSeconds must not be null");
        }

        int cappedSeconds = Math.min(MAX_HEARTBEAT_SECONDS, Math.max(1, activeSeconds));
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LearningActivityDaily activity = learningActivityDailyRepository
                .findByUserIdAndCourseIdAndActivityDate(userId, courseId, today)
                .orElseGet(() -> LearningActivityDaily.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .activityDate(today)
                        .activeSeconds(0L)
                        .lastActivityAt(now)
                        .build());
        activity.setActiveSeconds((activity.getActiveSeconds() != null ? activity.getActiveSeconds() : 0L) + cappedSeconds);
        activity.setLastActivityAt(now);
        if (itemId != null && !itemId.isBlank()) {
            activity.setLastItemId(itemId.trim());
        }
        learningActivityDailyRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserLearningSummaryView getUserLearningSummary(String userId) {
        List<CourseProgressInfo> progressList = progressLookup.courseProgressOfUser(userId);
        long completedCourseCount = progressList.stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getIsCompleted()))
                .count();
        double averageProgressPercent = progressList.stream()
                .mapToDouble(this::progressPercent)
                .average()
                .orElse(0.0);
        Long totalLearningSeconds = learningActivityDailyRepository.sumActiveSecondsByUserId(userId);
        return new UserLearningSummaryView(
                completedCourseCount,
                averageProgressPercent,
                totalLearningSeconds != null ? totalLearningSeconds : 0L
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LearningActivitySummaryView getMyActivitySummary(String userId) {
        // the same zone recordActivity writes activity dates in, so a day here is a stored day
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.systemDefault()));
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<String, Long> byDay = new LinkedHashMap<>();
        learningActivityDailyRepository.aggregateUserActivityByDay(userId, weekStart, weekEnd)
                .forEach(row -> byDay.put((String) row[0], ((Number) row[1]).longValue()));
        List<LearningActivitySummaryView.Day> days = weekStart.datesUntil(weekEnd.plusDays(1))
                .map(date -> new LearningActivitySummaryView.Day(date, byDay.getOrDefault(date.toString(), 0L)))
                .toList();
        Long total = learningActivityDailyRepository.sumActiveSecondsByUserId(userId);
        return new LearningActivitySummaryView(total != null ? total : 0L, weekStart, days);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgressView> getUserCourseProgress(String userId, List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        return progressLookup.courseProgressViewsOfUser(userId, courseIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningActivityByTimeView> getUserActivitySeries(String userId, LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        LearningActivityDateRange range = activityRangeResolver.resolve(groupBy, startDate, endDate);
        List<Object[]> rows = range.groupByDay()
                ? learningActivityDailyRepository.aggregateUserActivityByDay(userId, range.startDate(), range.endDate())
                : learningActivityDailyRepository.aggregateUserActivityByMonth(userId, range.startDate(), range.endDate());

        return fillActivityByLabel(range, rows).entrySet().stream()
                .map(entry -> new LearningActivityByTimeView(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CoursesProgressSummaryView getCoursesProgressSummary(List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return new CoursesProgressSummaryView(0L, 0L, 0.0, List.of());
        }

        Map<String, List<CourseProgressInfo>> progressByCourse = progressLookup.courseProgressOfCourses(courseIds)
                .stream()
                .collect(Collectors.groupingBy(CourseProgressInfo::getCourseId));

        List<CourseProgressSummaryView> courses = courseIds.stream()
                .distinct()
                .map(courseId -> toCourseSummary(courseId, progressByCourse.getOrDefault(courseId, List.of())))
                .toList();

        long totalStudentProgressCount = courses.stream().mapToLong(CourseProgressSummaryView::studentCount).sum();
        long completedStudentProgressCount = courses.stream().mapToLong(CourseProgressSummaryView::completedStudentCount).sum();
        double completionRate = totalStudentProgressCount > 0
                ? completedStudentProgressCount * 100.0 / totalStudentProgressCount
                : 0.0;
        return new CoursesProgressSummaryView(totalStudentProgressCount, completedStudentProgressCount, completionRate, courses);
    }

    private CourseProgressSummaryView toCourseSummary(String courseId, List<CourseProgressInfo> progressList) {
        long studentCount = progressList.size();
        long completedStudentCount = progressList.stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getIsCompleted()))
                .count();
        double averageProgressPercent = progressList.stream()
                .mapToDouble(this::progressPercent)
                .average()
                .orElse(0.0);
        double completionRate = studentCount > 0 ? completedStudentCount * 100.0 / studentCount : 0.0;
        return new CourseProgressSummaryView(courseId, studentCount, completedStudentCount, averageProgressPercent, completionRate);
    }

    private double progressPercent(CourseProgressInfo progress) {
        if (progress.getTotalItems() == null || progress.getTotalItems() <= 0) {
            return 0.0;
        }
        int completedItems = progress.getCompletedItems() != null ? progress.getCompletedItems() : 0;
        return completedItems * 100.0 / progress.getTotalItems();
    }

    private Map<String, Long> fillActivityByLabel(LearningActivityDateRange range, List<Object[]> rows) {
        Map<String, Long> activityByLabel = new LinkedHashMap<>();
        range.bucketLabels().forEach(label -> activityByLabel.put(label, 0L));
        rows.forEach(row -> activityByLabel.put((String) row[0], ((Number) row[1]).longValue()));
        return activityByLabel;
    }
}
