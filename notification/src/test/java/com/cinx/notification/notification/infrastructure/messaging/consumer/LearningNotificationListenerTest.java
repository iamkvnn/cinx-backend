package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.learning.CourseCompletedEvent;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningNotificationListenerTest {
    @Mock
    private INotificationDispatchService dispatchService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private Channel channel;

    private LearningNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new LearningNotificationListener(
                dispatchService,
                idempotencyService,
                new NotificationFormatter(),
                new ObjectMapper()
        );
    }

    @Test
    void courseCompletedPayloadNotifiesLearner() throws Exception {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .userId("user-1")
                .courseId("course-1")
                .courseTitle("Course title")
                .build();

        listener.handleCourseCompleted(event, channel, 10L, "message-1");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(captor.getValue().channels()).containsExactly("IN_APP", "PUSH");
        assertThat(userIds(captor.getValue())).containsExactly("user-1");
        verify(idempotencyService).markSuccess("message-1");
        verify(channel).basicAck(10L, false);
    }

    @Test
    void genericCourseCompletedPayloadDispatchesByEventTypeHeader() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", "user-1");
        payload.put("courseId", "course-1");
        payload.put("courseTitle", "Course title");

        listener.handleGenericLearningEvent(payload, channel, 11L, "message-2", "learning.course.completed");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(captor.getValue().channels()).containsExactly("IN_APP", "PUSH");
        assertThat(userIds(captor.getValue())).containsExactly("user-1");
        verify(idempotencyService).markSuccess("message-2");
        verify(channel).basicAck(11L, false);
    }

    @Test
    void genericUnsupportedPayloadIsAckedWithoutRetry() throws Exception {
        listener.handleGenericLearningEvent(Map.of("eventId", "event-1"), channel, 12L, "message-3", "UnknownEvent");

        verify(dispatchService, never()).dispatch(org.mockito.ArgumentMatchers.any());
        verify(channel).basicAck(12L, false);
    }

    @Test
    void studyReminderIsDeliveredWithTheTextAndLinkLearningChose() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", "user-1");
        payload.put("reason", "PLAN_SESSION_PENDING");
        payload.put("title", "Đến giờ học rồi");
        payload.put("body", "Hôm nay bạn có 25 phút cho «Vòng lặp».");
        payload.put("actionUrl", "/learning/c1?lessonId=l1");
        payload.put("writtenBy", "AI");
        payload.put("reminderDate", "2026-10-13");

        listener.handleGenericLearningEvent(payload, channel, 13L, "study-reminder:user-1:2026-10-13", "learning.study-reminder.due");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        NotificationContext context = captor.getValue();
        assertThat(context.channels()).containsExactly("IN_APP", "PUSH");
        assertThat(userIds(context)).containsExactly("user-1");
        assertThat(context.inAppPayload())
                .containsEntry("type", "STUDY_REMINDER")
                .containsEntry("title", "Đến giờ học rồi")
                .containsEntry("actionUrl", "/learning/c1?lessonId=l1");
        verify(idempotencyService).markSuccess("study-reminder:user-1:2026-10-13");
        verify(channel).basicAck(13L, false);
    }

    @Test
    void subtitleReadyForReviewTellsTheRequesterInTheAppOnly() throws Exception {
        Map<String, Object> payload = subtitlePayload("REVIEW_REQUIRED", 2);

        listener.handleGenericLearningEvent(payload, channel, 14L, "subtitle-job:job-1:REVIEW_REQUIRED", "learning.subtitle.job.finished");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        NotificationContext context = captor.getValue();
        assertThat(context.channels()).containsExactly("IN_APP");
        assertThat(userIds(context)).containsExactly("instructor-1");
        assertThat(context.inAppPayload())
                .containsEntry("type", "SUBTITLE_REVIEW")
                .containsEntry("title", "Phụ đề cần duyệt")
                .containsEntry("actionUrl", "/instructor/courses/c1/edit?lesson=l1");
        assertThat((String) context.inAppPayload().get("message")).contains("«Tiếng Việt»", "«Vòng lặp»", "2 cảnh báo");
        verify(idempotencyService).markSuccess("subtitle-job:job-1:REVIEW_REQUIRED");
        verify(channel).basicAck(14L, false);
    }

    @Test
    void subtitleFailureSaysSo() throws Exception {
        listener.handleGenericLearningEvent(subtitlePayload("FAILED", 0), channel, 15L, "subtitle-job:job-1:FAILED", "learning.subtitle.job.finished");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(captor.getValue().inAppPayload())
                .containsEntry("type", "SUBTITLE_FAILED")
                .containsEntry("title", "Tạo phụ đề không thành công");
        verify(channel).basicAck(15L, false);
    }

    @Test
    void aRedeliveredSubtitleEventIsANoOp() throws Exception {
        org.mockito.Mockito.when(idempotencyService.isProcessed("subtitle-job:job-1:FAILED")).thenReturn(true);

        listener.handleGenericLearningEvent(subtitlePayload("FAILED", 0), channel, 16L, "subtitle-job:job-1:FAILED", "learning.subtitle.job.finished");

        verify(dispatchService, never()).dispatch(org.mockito.ArgumentMatchers.any());
        verify(channel).basicAck(16L, false);
    }

    private Map<String, Object> subtitlePayload(String outcome, int warningCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", "job-1");
        payload.put("userId", "instructor-1");
        payload.put("courseId", "c1");
        payload.put("lessonId", "l1");
        payload.put("lessonTitle", "Vòng lặp");
        payload.put("languageCode", "vi");
        payload.put("displayName", "Tiếng Việt");
        payload.put("outcome", outcome);
        payload.put("warningCount", warningCount);
        payload.put("actionUrl", "/instructor/courses/c1/edit?lesson=l1");
        return payload;
    }

    @Test
    void aLabMachineReadyAfterTheQueueReachesTheLearnerWithTheLessonLink() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>(Map.of("userId", "u1", "sessionId", "s1", "lessonId", "l1", "courseId", "c1",
                "lessonTitle", "Lab nginx"));

        listener.handleGenericLearningEvent(payload, channel, 20L, "lab-session-ready:s1", "LabSessionReady");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        NotificationContext context = captor.getValue();
        assertThat(context.channels()).containsExactly("IN_APP", "PUSH");
        assertThat(userIds(context)).containsExactly("u1");
        assertThat(context.inAppPayload()).containsEntry("type", "LAB_SESSION_READY").containsEntry("actionUrl", "/learning/c1?lessonId=l1");
        assertThat((String) context.inAppPayload().get("message")).contains("Lab nginx");
        verify(channel).basicAck(20L, false);
    }

    @Test
    void theTwoMinuteWarningSaysToSubmitNow() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>(Map.of("userId", "u1", "sessionId", "s1", "lessonId", "l1", "courseId", "c1",
                "lessonTitle", "Lab nginx", "minutesLeft", 2));

        listener.handleGenericLearningEvent(payload, channel, 21L, "lab-session-ending:s1:2", "LabSessionEnding");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(captor.getValue().inAppPayload()).containsEntry("type", "LAB_SESSION_ENDING").containsEntry("title", "Máy lab còn 2 phút");
        assertThat((String) captor.getValue().inAppPayload().get("message")).contains("Nộp bài ngay");
    }

    @Test
    void aRejectedRootReleaseTellsTheInstructorWhyWithTheStudioLink() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>(Map.of("instructorId", "i1", "lessonId", "l1", "courseId", "c1", "lessonTitle", "Lab root",
                "releaseId", "r1", "decision", "REJECTED", "reason", "Script tải từ host lạ."));

        listener.handleGenericLearningEvent(payload, channel, 22L, "lab-review-decided:r1:REJECTED", "LabReviewDecided");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        NotificationContext context = captor.getValue();
        assertThat(userIds(context)).containsExactly("i1");
        assertThat(context.inAppPayload()).containsEntry("type", "LAB_REVIEW_REJECTED").containsEntry("actionUrl", "/instructor/courses/c1/edit?lesson=l1");
        assertThat((String) context.inAppPayload().get("message")).contains("Script tải từ host lạ.");
    }

    @Test
    void aLabEventDeliveredTwiceIsSentOnce() throws Exception {
        when(idempotencyService.isProcessed("lab-session-ready:s1")).thenReturn(true);

        listener.handleGenericLearningEvent(new LinkedHashMap<>(Map.of("userId", "u1", "sessionId", "s1")), channel, 23L, "lab-session-ready:s1", "LabSessionReady");

        verify(dispatchService, never()).dispatch(org.mockito.ArgumentMatchers.any());
        verify(channel).basicAck(23L, false);
    }

    @SuppressWarnings("unchecked")
    private List<String> userIds(NotificationContext context) {
        return (List<String>) context.inAppPayload().get("userIds");
    }
}
