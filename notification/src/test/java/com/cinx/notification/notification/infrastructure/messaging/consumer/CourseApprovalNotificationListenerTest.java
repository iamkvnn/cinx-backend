package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.IdentityClient;
import com.cinx.notification.notification.infrastructure.client.UserClient;
import com.cinx.notification.notification.infrastructure.client.dto.user.UserDto;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.course.CourseApprovalRequestedEvent;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseApprovalNotificationListenerTest {
    @Mock UserClient userClient;
    @Mock IdentityClient identityClient;
    @Mock INotificationDispatchService dispatchService;
    @Mock IdempotencyService idempotencyService;
    @Mock Channel channel;
    CourseApprovalNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CourseApprovalNotificationListener(
                userClient,
                identityClient,
                dispatchService,
                idempotencyService,
                new NotificationFormatter());
    }

    @Test
    void resolvesAdminRecipientsFromAuthIdentityOwner() throws Exception {
        when(identityClient.getCourseReviewerUserIds())
                .thenReturn(ApiResponse.success("ok", List.of("admin-1", "admin-2")));
        when(userClient.getUserById("instructor-1"))
                .thenReturn(ApiResponse.success("ok", new UserDto("instructor-1", "Instructor", "instructor@example.com")));

        listener.handleCourseApprovalRequested(
                new CourseApprovalRequestedEvent("course-1", "Course", "instructor-1"),
                channel,
                10L,
                "message-1");

        ArgumentCaptor<NotificationContext> context = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(context.capture());
        assertThat(context.getValue().inAppPayload().get("userIds"))
                .isEqualTo(List.of("admin-1", "admin-2"));
        verify(idempotencyService).markSuccess("message-1");
        verify(channel).basicAck(10L, false);
    }
}
