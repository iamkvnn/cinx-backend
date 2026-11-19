package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserNotificationListenerTest {
    @Test
    void projectionOnlyUserEventIsAcknowledgedWithoutDispatch() throws Exception {
        INotificationDispatchService dispatch = mock(INotificationDispatchService.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);
        Channel channel = mock(Channel.class);
        UserNotificationListener listener = new UserNotificationListener(dispatch, idempotency);

        listener.handleUserEvent(
                Map.of("payload", Map.of("userId", "user-1")),
                channel,
                11L,
                "user.preference.updated",
                "message-2");

        verify(dispatch, never()).dispatch(any());
        verify(idempotency).markSuccess("message-2");
        verify(channel).basicAck(11L, false);
    }

    @Test
    void friendEventCopyIsAckedWithoutTouchingTheSharedInbox() throws Exception {
        INotificationDispatchService dispatch = mock(INotificationDispatchService.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);
        Channel channel = mock(Channel.class);
        UserNotificationListener listener = new UserNotificationListener(dispatch, idempotency);

        listener.handleUserEvent(Map.of("requestId", "r-1"), channel, 12L, "user.friend.requested", "message-3");

        verify(dispatch, never()).dispatch(any());
        verifyNoInteractions(idempotency);
        verify(channel).basicAck(12L, false);
    }
}
