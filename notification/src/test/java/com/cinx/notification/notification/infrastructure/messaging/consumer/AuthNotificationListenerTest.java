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

class AuthNotificationListenerTest {
    @Test
    void projectionOnlyAuthEventIsAcknowledgedWithoutDispatch() throws Exception {
        INotificationDispatchService dispatch = mock(INotificationDispatchService.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);
        Channel channel = mock(Channel.class);
        AuthNotificationListener listener = new AuthNotificationListener(dispatch, idempotency);

        listener.handleAuthEvent(
                Map.of("userId", "user-1", "status", "BANNED"),
                channel,
                10L,
                "auth.authorization.changed",
                "message-1");

        verify(dispatch, never()).dispatch(any());
        verify(idempotency).markSuccess("message-1");
        verify(channel).basicAck(10L, false);
    }
}
