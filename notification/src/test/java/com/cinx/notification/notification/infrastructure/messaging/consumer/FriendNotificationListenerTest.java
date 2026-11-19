package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.user.FriendEventNotice;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** user.friend.requested and user.friend.accepted become FRIEND_REQUEST and FRIEND_ACCEPTED, in the app, to one person. */
@ExtendWith(MockitoExtension.class)
class FriendNotificationListenerTest {
    @Mock
    private INotificationDispatchService dispatchService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private Channel channel;

    private FriendNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new FriendNotificationListener(dispatchService, idempotencyService, new NotificationFormatter());
    }

    @Test
    void aRequestReachesTheRecipientInTheApp() throws Exception {
        listener.handle(requested("Lan <Anh>"), channel, 1L, "msg-1", "user.friend.requested");

        NotificationContext ctx = dispatched();
        assertThat(ctx.channels()).containsExactly("IN_APP");
        assertThat(ctx.pushPayload()).isNull();
        assertThat(ctx.inAppPayload())
                .containsEntry("type", "FRIEND_REQUEST")
                .containsEntry("userIds", List.of("binh"))
                .containsEntry("referenceId", "r-1")
                .containsEntry("actionUrl", "/user-profile?tab=friends")
                .containsEntry("title", "Lời mời kết bạn");
        assertThat((String) ctx.inAppPayload().get("message")).startsWith("Lan <Anh> muốn kết bạn với bạn.");
        verify(idempotencyService).markSuccess("msg-1");
        verify(channel).basicAck(1L, false);
    }

    @Test
    void anAcceptReachesTheOneWhoAsked() throws Exception {
        FriendEventNotice event = FriendEventNotice.builder()
                .requestId("r-1").accepterId("binh").accepterName("Bình").requesterId("an").build();

        listener.handle(event, channel, 2L, "msg-2", "user.friend.accepted");

        NotificationContext ctx = dispatched();
        assertThat(ctx.inAppPayload())
                .containsEntry("type", "FRIEND_ACCEPTED")
                .containsEntry("userIds", List.of("an"))
                .containsEntry("actionUrl", "/user-profile?tab=friends");
        assertThat((String) ctx.inAppPayload().get("message")).startsWith("Bình đã chấp nhận lời mời kết bạn của bạn.");
        verify(channel).basicAck(2L, false);
    }

    @Test
    void aRedeliveredMessageIsANoOp() throws Exception {
        when(idempotencyService.isProcessed("msg-3")).thenReturn(true);

        listener.handle(requested("Lan"), channel, 3L, "msg-3", "user.friend.requested");

        verify(dispatchService, never()).dispatch(any());
        verify(channel).basicAck(3L, false);
    }

    @Test
    void noRecipientOrAnUnknownKeyIsAckedWithoutSending() throws Exception {
        FriendEventNotice nobody = requested("Lan");
        nobody.setToUserId(" ");

        listener.handle(nobody, channel, 4L, "msg-4", "user.friend.requested");
        listener.handle(requested("Lan"), channel, 5L, "msg-5", "user.friend.declined");

        verify(dispatchService, never()).dispatch(any());
        verify(channel).basicAck(4L, false);
        verify(channel).basicAck(5L, false);
    }

    @Test
    void aFailedDispatchIsDeadLetteredAndNotMarked() throws Exception {
        doThrow(new RuntimeException("db down")).when(dispatchService).dispatch(any());

        listener.handle(requested("Lan"), channel, 6L, "msg-6", "user.friend.requested");

        verify(idempotencyService, never()).markSuccess(any());
        verify(channel).basicNack(6L, false, false);
    }

    @Test
    void aBlankNameReadsAsAClassmateAndTheEmailBodyIsEscaped() {
        NotificationFormatter formatter = new NotificationFormatter();

        NotificationMessage request = formatter.friendRequested("r-1", "an", " ");
        NotificationMessage accepted = formatter.friendAccepted("r-1", "binh", "<b>Bình</b>");

        assertThat(request.message()).startsWith("Một bạn học muốn kết bạn");
        assertThat(request.metadata()).containsEntry("fromUserId", "an").containsEntry("requestId", "r-1");
        assertThat(accepted.title()).isEqualTo("Hai bạn đã là bạn bè");
        assertThat(accepted.htmlBody()).contains("&lt;b&gt;").doesNotContain("<b>");
    }

    private FriendEventNotice requested(String fromName) {
        return FriendEventNotice.builder().requestId("r-1").fromUserId("an").fromName(fromName).toUserId("binh").build();
    }

    private NotificationContext dispatched() {
        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        return captor.getValue();
    }
}
