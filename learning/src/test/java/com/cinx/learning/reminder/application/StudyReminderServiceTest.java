package com.cinx.learning.reminder.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.engagement.application.EngagementLookup;
import com.cinx.learning.progress.application.ProgressLookup;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.reminder.domain.ReminderAuthor;
import com.cinx.learning.reminder.domain.ReminderReason;
import com.cinx.learning.reminder.domain.StudyReminderLogRepository;
import com.cinx.learning.reminder.domain.StudyReminderPreference;
import com.cinx.learning.reminder.domain.StudyReminderPreferenceRepository;
import com.cinx.learning.review.application.IReviewService;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.studyplan.application.IStudyPlanService;
import com.cinx.learning.studyplan.application.query.LastActivityView;
import com.cinx.learning.studyplan.application.query.StudyPlanView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyReminderServiceTest {
    // 12:00 UTC is 19:00 in Asia/Ho_Chi_Minh, the default reminder hour
    private static final Instant AT_19 = Instant.parse("2026-10-13T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    @Mock private IStudyPlanService studyPlans;
    @Mock private EngagementLookup engagement;
    @Mock private ProgressLookup progress;
    @Mock private StudyReminderPreferenceRepository preferences;
    @Mock private StudyReminderLogRepository logs;
    @Mock private ReminderTextWriter writer;
    @Mock private StudyReminderSender sender;
    @Mock private IReviewService reviews;

    private StudyReminderService service;

    @BeforeEach
    void setUp() {
        service = at(AT_19);
        when(studyPlans.get(anyString(), any())).thenThrow(new NotFoundException("no plan"));
        when(studyPlans.lastActivity(anyString(), any())).thenThrow(new NotFoundException("nothing yet"));
        when(writer.rewrite(any())).thenReturn(Optional.empty());
    }

    private StudyReminderService at(Instant instant) {
        return new StudyReminderService(Clock.fixed(instant, ZoneOffset.UTC), studyPlans, engagement, progress, preferences,
                logs, writer, sender, reviews);
    }

    private ReminderMessage sentTo(String userId, ReminderAuthor author) {
        ArgumentCaptor<ReminderMessage> message = ArgumentCaptor.forClass(ReminderMessage.class);
        verify(sender).send(eq(userId), eq(TODAY), message.capture(), eq(author));
        return message.getValue();
    }

    private void planWithPendingLesson(String userId) {
        StudyPlanView.Session session = new StudyPlanView.Session("c1", "Python", "l1", "Vòng lặp", "Chương 1", LessonType.VIDEO, 25, "PLANNED");
        StudyPlanView.Day day = new StudyPlanView.Day(TODAY, true, false, 30, List.of(session));
        StudyPlanView plan = new StudyPlanView(null, 30, List.of(1), TODAY, TODAY, TODAY, TODAY, 0, 0, 1, 25, null, List.of(),
                new StudyPlanView.Week(TODAY, TODAY, List.of(day)));
        when(studyPlans.userIdsWithPlan()).thenReturn(List.of(userId));
        when(studyPlans.get(eq(userId), any())).thenReturn(plan);
    }

    @Test
    void nothingIsSentDuringQuietHours() {
        assertThat(at(Instant.parse("2026-10-13T15:00:00Z")).runHour()).isZero();
        verifyNoInteractions(studyPlans, engagement, sender);
    }

    @Test
    void aPendingPlanLessonIsRemindedWithItsLinkAndTheAssistantsWording() {
        planWithPendingLesson("u1");
        when(writer.rewrite(any())).thenReturn(Optional.of(new ReminderTextWriter.Text("Học thôi", "Vòng lặp đang đợi bạn.")));

        assertThat(service.runHour()).isEqualTo(1);

        ReminderMessage message = sentTo("u1", ReminderAuthor.AI);
        assertThat(message.reason()).isEqualTo(ReminderReason.PLAN_SESSION_PENDING);
        assertThat(message.actionUrl()).isEqualTo("/learning/c1?lessonId=l1");
        assertThat(message.title()).isEqualTo("Học thôi");
    }

    @Test
    void theTemplateGoesWhenTheAssistantCannotHelp() {
        planWithPendingLesson("u1");

        service.runHour();

        assertThat(sentTo("u1", ReminderAuthor.TEMPLATE).body()).contains("25 phút", "«Vòng lặp»");
    }

    @Test
    void aLearnerWhoSwitchedOffOrChoseAnotherHourIsSkipped() {
        when(studyPlans.userIdsWithPlan()).thenReturn(List.of("off", "morning"));
        when(preferences.findByUserIds(any())).thenReturn(List.of(
                StudyReminderPreference.builder().userId("off").enabled(false).hour(19).build(),
                StudyReminderPreference.builder().userId("morning").enabled(true).hour(8).build()));

        assertThat(service.runHour()).isZero();
        verifyNoInteractions(sender);
    }

    @Test
    void aLearnerAlreadyRemindedOrWhoStudiedTodayIsSkipped() {
        planWithPendingLesson("u1");
        when(engagement.activeOn("u1", TODAY)).thenReturn(true);
        assertThat(service.runHour()).isZero();

        when(engagement.activeOn("u1", TODAY)).thenReturn(false);
        when(logs.existsByUserIdAndReminderDate("u1", TODAY)).thenReturn(true);
        assertThat(service.runHour()).isZero();
        verify(sender, never()).send(any(), any(), any(), any());
    }

    @Test
    void noMoreThanFourRemindersInAnySevenDays() {
        planWithPendingLesson("u1");
        when(logs.countByUserIdAndReminderDateBetween("u1", TODAY.minusDays(6), TODAY.minusDays(1))).thenReturn(3L);
        assertThat(service.runHour()).isEqualTo(1);

        when(logs.countByUserIdAndReminderDateBetween("u1", TODAY.minusDays(6), TODAY.minusDays(1))).thenReturn(4L);
        assertThat(service.runHour()).isZero();
    }

    @Test
    void aStreakExtendedYesterdayIsKeptAlive() {
        when(engagement.userIdsWithStreakAtRisk(TODAY, 2)).thenReturn(List.of("u2"));
        when(engagement.currentStreak("u2")).thenReturn(4);
        when(engagement.lastActiveDay("u2")).thenReturn(Optional.of(TODAY.minusDays(1)));

        service.runHour();

        ReminderMessage message = sentTo("u2", ReminderAuthor.TEMPLATE);
        assertThat(message.reason()).isEqualTo(ReminderReason.STREAK_AT_RISK);
        assertThat(message.actionUrl()).isEqualTo("/week");
        assertThat(message.title()).contains("4 ngày");
    }

    @Test
    void dueReviewsAreRemindedWithTheCountCappedAtADaysQueue() {
        when(reviews.userIdsWithDue(TODAY)).thenReturn(List.of("u5"));
        when(reviews.dueCount("u5", TODAY)).thenReturn(27L);

        service.runHour();

        ReminderMessage message = sentTo("u5", ReminderAuthor.TEMPLATE);
        assertThat(message.reason()).isEqualTo(ReminderReason.REVIEWS_DUE);
        assertThat(message.actionUrl()).isEqualTo("/review");
        assertThat(message.title()).startsWith("20 câu");
        assertThat(message.facts()).containsEntry("questions", 20);
    }

    @Test
    void aStreakAtRiskComesBeforeDueReviews() {
        when(engagement.userIdsWithStreakAtRisk(TODAY, 2)).thenReturn(List.of("u2"));
        when(engagement.currentStreak("u2")).thenReturn(4);
        when(engagement.lastActiveDay("u2")).thenReturn(Optional.of(TODAY.minusDays(1)));
        when(reviews.dueCount("u2", TODAY)).thenReturn(3L);

        service.runHour();

        assertThat(sentTo("u2", ReminderAuthor.TEMPLATE).reason()).isEqualTo(ReminderReason.STREAK_AT_RISK);
    }

    @Test
    void anInactiveLearnerIsBroughtBackOnlyToAnUnfinishedCourse() {
        when(engagement.userIdsLastActiveBetween(TODAY.minusDays(30), TODAY.minusDays(3))).thenReturn(List.of("u3"));
        when(engagement.lastActiveDay("u3")).thenReturn(Optional.of(TODAY.minusDays(5)));
        doReturn(new LastActivityView("c1", "Python", "l2", "Hàm", "Chương 2", LessonType.VIDEO, null, 10, null))
                .when(studyPlans).lastActivity("u3", null);
        when(progress.courseProgressOfUser("u3", List.of("c1"))).thenReturn(List.of(progressOf(false)));

        service.runHour();
        ReminderMessage message = sentTo("u3", ReminderAuthor.TEMPLATE);
        assertThat(message.reason()).isEqualTo(ReminderReason.INACTIVE);
        assertThat(message.body()).contains("5 ngày", "«Python»");

        when(progress.courseProgressOfUser("u3", List.of("c1"))).thenReturn(List.of(progressOf(true)));
        assertThat(service.runHour()).isZero();
    }

    @Test
    void aDuplicateDayFromAnotherRunIsNotCounted() {
        planWithPendingLesson("u1");
        doThrow(new DataIntegrityViolationException("uk_study_reminder_log_user_date"))
                .when(sender).send(any(), any(), any(), any());

        assertThat(service.runHour()).isZero();
    }

    private CourseProgressInfo progressOf(boolean completed) {
        return CourseProgressInfo.builder().courseId("c1").userId("u3").isCompleted(completed).build();
    }
}
