package com.cinx.learning.engagement.application;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.courseview.application.query.CourseView;
import com.cinx.learning.engagement.application.query.CourseEngagementOverviewView;
import com.cinx.learning.engagement.application.query.LearningActivityByTimeView;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.engagement.domain.LearningActivityDailyRepository;
import com.cinx.learning.engagement.domain.LearningActivityGroupBy;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.catalog.application.ICourseAccessService;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearningEngagementStatisticsService implements ILearningEngagementStatisticsService {
    private final ProgressLookup progressLookup;
    private final LearningActivityDailyRepository learningActivityDailyRepository;
    private final CourseService courseService;
    private final ICourseAccessService courseAccessService;
    private final LearningActivityRangeResolver activityRangeResolver = new LearningActivityRangeResolver();

    @Override
    @Transactional(readOnly = true)
    public CourseEngagementOverviewView getInstructorCourseEngagement(String currentUserId, String courseId, LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        CourseView course = courseService.getCourseById(courseId).data();
        if (!courseAccessService.hasCourseCapability(currentUserId, courseId, CourseMembershipRole.MANAGER)) {
            throw new BadRequestException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED, "Only an owner or course manager can view this engagement overview");
        }
        return buildCourseEngagement(courseId, groupBy, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseEngagementOverviewView getAdminCourseEngagement(String courseId, LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        return buildCourseEngagement(courseId, groupBy, startDate, endDate);
    }

    private CourseEngagementOverviewView buildCourseEngagement(String courseId, LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        LearningActivityDateRange range = activityRangeResolver.resolve(groupBy, startDate, endDate);
        List<Object[]> activityRows = range.groupByDay()
                ? learningActivityDailyRepository.aggregateCourseActivityByDay(courseId, range.startDate(), range.endDate())
                : learningActivityDailyRepository.aggregateCourseActivityByMonth(courseId, range.startDate(), range.endDate());
        Long totalLearningSeconds = learningActivityDailyRepository.sumActiveSecondsByCourseIdBetween(courseId, range.startDate(), range.endDate());
        Long activeLearners = learningActivityDailyRepository.countActiveLearnersByCourseIdBetween(courseId, range.startDate(), range.endDate());
        List<CourseProgressInfo> progressList = progressLookup.courseProgressOfCourse(courseId);
        return new CourseEngagementOverviewView(
                activeLearners != null ? activeLearners : 0L,
                totalLearningSeconds != null ? totalLearningSeconds : 0L,
                averageProgressPercent(progressList),
                completionRate(progressList),
                fillActivityByTime(range, activityRows)
        );
    }

    private List<LearningActivityByTimeView> fillActivityByTime(LearningActivityDateRange range, List<Object[]> rows) {
        Map<String, Long> valuesByLabel = new LinkedHashMap<>();
        range.bucketLabels().forEach(label -> valuesByLabel.put(label, 0L));
        rows.forEach(row -> valuesByLabel.put((String) row[0], ((Number) row[1]).longValue()));
        return valuesByLabel.entrySet().stream()
                .map(entry -> new LearningActivityByTimeView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Double averageProgressPercent(List<CourseProgressInfo> progressList) {
        return progressList.stream()
                .mapToDouble(this::progressPercent)
                .average()
                .orElse(0.0);
    }

    private Double completionRate(List<CourseProgressInfo> progressList) {
        if (progressList.isEmpty()) {
            return 0.0;
        }
        long completedCount = progressList.stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getIsCompleted()))
                .count();
        return completedCount * 100.0 / progressList.size();
    }

    private double progressPercent(CourseProgressInfo progress) {
        if (progress.getTotalItems() == null || progress.getTotalItems() <= 0) {
            return 0.0;
        }
        int completedItems = progress.getCompletedItems() != null ? progress.getCompletedItems() : 0;
        return completedItems * 100.0 / progress.getTotalItems();
    }
}
