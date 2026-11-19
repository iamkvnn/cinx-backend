package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.CourseClient;
import com.cinx.notification.notification.infrastructure.client.UserClient;
import com.cinx.notification.notification.infrastructure.client.dto.course.CourseResponse;
import com.cinx.notification.notification.infrastructure.client.dto.course.InstructorResponse;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.social.CourseAnswerCreatedEvent;
import com.cinx.notification.notification.infrastructure.messaging.event.social.CourseQuestionCreatedEvent;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class SocialNotificationListenerTest {
    @Mock
    private CourseClient courseClient;
    @Mock
    private UserClient userClient;
    @Mock
    private INotificationDispatchService dispatchService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private Channel channel;

    private SocialNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new SocialNotificationListener(
                courseClient,
                userClient,
                dispatchService,
                idempotencyService,
                new NotificationFormatter(),
                new ObjectMapper()
        );
    }

    @Test
    void questionCreatedPayloadNotifiesCourseInstructor() throws Exception {
        when(courseClient.getCourseById("course-1")).thenReturn(new ApiResponse<>(
                true, "ok", new CourseResponse("course-1", "Course title", new InstructorResponse("instructor-1", "Instructor"))));

        CourseQuestionCreatedEvent event = CourseQuestionCreatedEvent.builder()
                .eventId("event-1")
                .questionId("question-1")
                .courseId("course-1")
                .askedByUserId("student-1")
                .questionTitle("Question title")
                .build();

        listener.handleQuestionCreated(event, channel, 10L, "message-1");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(captor.getValue().channels()).containsExactly("IN_APP");
        assertThat(userIds(captor.getValue())).containsExactly("instructor-1");
        verify(idempotencyService).markSuccess("message-1");
        verify(channel).basicAck(10L, false);
    }

    @Test
    void answerCreatedPayloadNotifiesQuestionAuthor() throws Exception {
        CourseAnswerCreatedEvent event = CourseAnswerCreatedEvent.builder()
                .eventId("event-2")
                .answerId("answer-1")
                .questionId("question-1")
                .courseId("course-1")
                .questionAuthorId("student-1")
                .answeredByUserId("instructor-1")
                .build();

        listener.handleAnswerCreated(event, channel, 11L, "message-2");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(userIds(captor.getValue())).containsExactly("student-1");
        verify(idempotencyService).markSuccess("message-2");
        verify(channel).basicAck(11L, false);
    }

    @Test
    void replyPayloadNotifiesParentAnswerAuthor() throws Exception {
        CourseAnswerCreatedEvent event = CourseAnswerCreatedEvent.builder()
                .eventId("event-3")
                .answerId("answer-2")
                .questionId("question-1")
                .courseId("course-1")
                .questionAuthorId("student-1")
                .parentAnswerAuthorId("parent-author-1")
                .answeredByUserId("student-2")
                .build();

        listener.handleAnswerCreated(event, channel, 12L, "message-3");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(userIds(captor.getValue())).containsExactly("parent-author-1");
        verify(channel).basicAck(12L, false);
    }

    @Test
    void answerCreatedDoesNotNotifyActorThemself() throws Exception {
        CourseAnswerCreatedEvent event = CourseAnswerCreatedEvent.builder()
                .eventId("event-4")
                .answerId("answer-1")
                .questionId("question-1")
                .courseId("course-1")
                .questionAuthorId("student-1")
                .answeredByUserId("student-1")
                .build();

        listener.handleAnswerCreated(event, channel, 13L, "message-4");

        verify(dispatchService, never()).dispatch(org.mockito.ArgumentMatchers.any());
        verify(idempotencyService).markSuccess("message-4");
        verify(channel).basicAck(13L, false);
    }

    @Test
    void genericQuestionCreatedPayloadDispatchesByEventTypeHeader() throws Exception {
        when(courseClient.getCourseById("course-1")).thenReturn(new ApiResponse<>(
                true, "ok", new CourseResponse("course-1", "Course title", new InstructorResponse("instructor-1", "Instructor"))));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", "event-5");
        payload.put("questionId", "question-1");
        payload.put("courseId", "course-1");
        payload.put("askedByUserId", "student-1");
        payload.put("questionTitle", "Question title");

        listener.handleGenericSocialEvent(payload, channel, 14L, "message-5", "CourseQuestionCreated");

        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        assertThat(userIds(captor.getValue())).containsExactly("instructor-1");
        verify(idempotencyService).markSuccess("message-5");
        verify(channel).basicAck(14L, false);
    }

    @Test
    void genericUnsupportedPayloadIsAckedWithoutRetry() throws Exception {
        listener.handleGenericSocialEvent(Map.of("eventId", "event-6"), channel, 15L, "message-6", "UnknownEvent");

        verify(dispatchService, never()).dispatch(org.mockito.ArgumentMatchers.any());
        verify(channel).basicAck(15L, false);
    }

    @SuppressWarnings("unchecked")
    private List<String> userIds(NotificationContext context) {
        return (List<String>) context.inAppPayload().get("userIds");
    }
}
