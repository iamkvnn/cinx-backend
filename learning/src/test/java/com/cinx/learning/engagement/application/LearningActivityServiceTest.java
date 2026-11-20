package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.application.command.LearningActivityCommand;
import com.cinx.learning.engagement.application.query.LearningActivityByTimeView;
import com.cinx.learning.engagement.application.query.LearningActivitySummaryView;
import com.cinx.learning.engagement.application.query.UserLearningSummaryView;
import com.cinx.learning.engagement.domain.LearningActivityDaily;
import com.cinx.learning.engagement.domain.LearningActivityDailyRepository;
import com.cinx.learning.engagement.domain.LearningActivityGroupBy;
import com.cinx.learning.progress.application.ProgressLookup;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.progress.application.query.CoursesProgressSummaryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningActivityServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ProgressLookup progressLookup;
    @Mock
    private LearningActivityDailyRepository learningActivityDailyRepository;

    private LearningActivityService learningActivityService;

    @BeforeEach
    void setUp() {
        learningActivityService = new LearningActivityService(progressLookup, learningActivityDailyRepository, CLOCK);
    }

    @Test
    void recordActivityCapsHeartbeatAtFiveMinutes() {
        when(learningActivityDailyRepository.findByUserIdAndCourseIdAndActivityDate("user-1", "course-1", LocalDate.now()))
                .thenReturn(Optional.empty());
        when(learningActivityDailyRepository.save(any(LearningActivityDaily.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        learningActivityService.recordActivity("user-1", new LearningActivityCommand("course-1", "lesson-1", 1_000));

        ArgumentCaptor<LearningActivityDaily> captor = ArgumentCaptor.forClass(LearningActivityDaily.class);
        verify(learningActivityDailyRepository).save(captor.capture());
        assertThat(captor.getValue().getActiveSeconds()).isEqualTo(300L);
        assertThat(captor.getValue().getLastActivityAt()).isNotNull();
        assertThat(captor.getValue().getLastItemId()).isEqualTo("lesson-1");
    }

    @Test
    void getUserLearningSummaryAggregatesProgressAndTime() {
        CourseProgressInfo first = progress("course-1", 10, 10, true);
        CourseProgressInfo second = progress("course-2", 5, 10, false);
        when(progressLookup.courseProgressOfUser("user-1")).thenReturn(List.of(first, second));
        when(learningActivityDailyRepository.sumActiveSecondsByUserId("user-1")).thenReturn(12_600L);

        UserLearningSummaryView summary = learningActivityService.getUserLearningSummary("user-1");

        // (100% + 50%) / 2 courses
        assertThat(summary.completedCourseCount()).isEqualTo(1L);
        assertThat(summary.averageProgressPercent()).isEqualTo(75.0);
        assertThat(summary.totalLearningSeconds()).isEqualTo(12_600L);
    }

    @Test
    void myActivitySummaryCoversTheCurrentWeekFromMonday() {
        when(learningActivityDailyRepository.aggregateUserActivityByDay("user-1", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-10-06", 600L}));
        when(learningActivityDailyRepository.sumActiveSecondsByUserId("user-1")).thenReturn(5_000L);

        LearningActivitySummaryView summary = learningActivityService.getMyActivitySummary("user-1");

        assertThat(summary.weekStart()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(summary.days()).hasSize(7);
        assertThat(summary.days().get(1).seconds()).isEqualTo(600L);
        assertThat(summary.days().get(0).seconds()).isZero();
        assertThat(summary.totalSeconds()).isEqualTo(5_000L);
    }

    @Test
    void getUserActivitySeriesFillsMissingMonthsWithZero() {
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(2);
        String middleMonth = startMonth.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        when(learningActivityDailyRepository.aggregateUserActivityByMonth(
                "user-1",
                startMonth.atDay(1),
                endMonth.atEndOfMonth()
        )).thenReturn(List.<Object[]>of(new Object[]{middleMonth, 3600L}));

        List<LearningActivityByTimeView> activity = learningActivityService.getUserActivitySeries(
                "user-1",
                LearningActivityGroupBy.MONTH,
                startMonth.atDay(1),
                endMonth.atEndOfMonth()
        );

        assertThat(activity).hasSize(3);
        assertThat(activity.get(0).activeSeconds()).isZero();
        assertThat(activity.get(1).timeLabel()).isEqualTo(middleMonth);
        assertThat(activity.get(1).activeSeconds()).isEqualTo(3600L);
        assertThat(activity.get(2).activeSeconds()).isZero();
    }

    @Test
    void getUserActivitySeriesRejectsMoreThanTwelveMonths() {
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(12);

        assertThatThrownBy(() -> learningActivityService.getUserActivitySeries(
                "user-1",
                LearningActivityGroupBy.MONTH,
                startMonth.atDay(1),
                endMonth.atEndOfMonth()
        ))
                .isInstanceOf(com.cinx.common.exception.BadRequestException.class);
    }

    @Test
    void getUserActivitySeriesFillsMissingDaysWithZero() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(2);
        String middleDay = startDate.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        when(learningActivityDailyRepository.aggregateUserActivityByDay("user-1", startDate, endDate))
                .thenReturn(List.<Object[]>of(new Object[]{middleDay, 900L}));

        List<LearningActivityByTimeView> activity = learningActivityService.getUserActivitySeries(
                "user-1",
                LearningActivityGroupBy.DAY,
                startDate,
                endDate
        );

        assertThat(activity).hasSize(3);
        assertThat(activity.get(0).activeSeconds()).isZero();
        assertThat(activity.get(1).timeLabel()).isEqualTo(middleDay);
        assertThat(activity.get(1).activeSeconds()).isEqualTo(900L);
        assertThat(activity.get(2).activeSeconds()).isZero();
    }

    @Test
    void getCoursesProgressSummaryAggregatesPerCourseAndTotals() {
        CourseProgressInfo first = progress("course-1", 10, 10, true);
        CourseProgressInfo second = progress("course-1", 5, 10, false);
        CourseProgressInfo third = progress("course-2", 8, 10, false);
        when(progressLookup.courseProgressOfCourses(List.of("course-1", "course-2")))
                .thenReturn(List.of(first, second, third));

        CoursesProgressSummaryView summary = learningActivityService.getCoursesProgressSummary(List.of("course-1", "course-2"));

        assertThat(summary.totalStudentProgressCount()).isEqualTo(3L);
        assertThat(summary.completedStudentProgressCount()).isEqualTo(1L);
        assertThat(summary.courses()).hasSize(2);
        // course-1: (100% + 50%) / 2 learners
        assertThat(summary.courses().get(0).averageProgressPercent()).isEqualTo(75.0);
    }

    private CourseProgressInfo progress(String courseId, int completedItems, int totalItems, boolean completed) {
        return CourseProgressInfo.builder()
                .courseId(courseId)
                .completedItems(completedItems)
                .totalItems(totalItems)
                .isCompleted(completed)
                .build();
    }
}
