package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.campus.CampusEventNotice;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusNotificationListenerTest {
    private static final Instant STARTS = Instant.parse("2026-11-13T13:00:00Z");

    @Mock
    private INotificationDispatchService dispatchService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private Channel channel;

    private CampusNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CampusNotificationListener(dispatchService, idempotencyService, new NotificationFormatter());
    }

    @Test
    void aNewEventReachesEveryRecipientInTheAppOnce() throws Exception {
        listener.handle(notice("u-1", "u-2", "u-1"), channel, 1L, "msg-1", "campus.event.scheduled");

        NotificationContext ctx = dispatched();
        assertThat(ctx.channels()).containsExactly("IN_APP");
        assertThat(userIds(ctx)).containsExactly("u-1", "u-2");
        assertThat(ctx.inAppPayload())
                .containsEntry("type", "CAMPUS_EVENT_SCHEDULED")
                .containsEntry("referenceId", "ev-1")
                .containsEntry("actionUrl", "/campus?zone=z-1");
        assertThat((String) ctx.inAppPayload().get("message")).contains("\"Chữa bài SQL\"", "20:00 thứ Sáu 13/11");
        verify(idempotencyService).markSuccess("msg-1");
        verify(channel).basicAck(1L, false);
    }

    @Test
    void theReminderIsAlsoPushed() throws Exception {
        listener.handle(notice("u-1"), channel, 2L, "msg-2", "campus.event.reminder.due");

        NotificationContext ctx = dispatched();
        assertThat(ctx.channels()).containsExactly("IN_APP", "PUSH");
        assertThat(ctx.inAppPayload()).containsEntry("type", "CAMPUS_EVENT_REMINDER");
        assertThat(ctx.pushPayload()).containsEntry("userIds", List.of("u-1"));
        verify(channel).basicAck(2L, false);
    }

    @Test
    void aCancellationTellsWhoRangTheBell() throws Exception {
        CampusEventNotice event = notice("u-3");
        event.setKind(null);

        listener.handle(event, channel, 3L, "msg-3", "campus.event.cancelled");

        NotificationContext ctx = dispatched();
        assertThat(ctx.channels()).containsExactly("IN_APP", "PUSH");
        assertThat(ctx.inAppPayload())
                .containsEntry("type", "CAMPUS_EVENT_CANCELLED")
                .containsEntry("title", "Sự kiện đã bị hủy");
        verify(channel).basicAck(3L, false);
    }

    @Test
    void aReplyTellsTheThreadsAuthorInTheAppAndOpensTheThread() throws Exception {
        CampusEventNotice reply = CampusEventNotice.builder()
                .threadId("t-1").replyId("r-1").placeKind("club").placeId("c-1").placeTitle("Lớp Thảo").title("Ôn chương 3")
                .replierId("u-2").replierName("Minh").recipientIds(List.of("u-1")).build();

        listener.handle(reply, channel, 8L, "msg-8", "campus.thread.replied");

        NotificationContext ctx = dispatched();
        assertThat(ctx.channels()).containsExactly("IN_APP");
        assertThat(userIds(ctx)).containsExactly("u-1");
        assertThat(ctx.inAppPayload())
                .containsEntry("type", "CAMPUS_THREAD_REPLY")
                .containsEntry("referenceId", "t-1")
                .containsEntry("actionUrl", "/campus?zone=c-1&thread=t-1");
        assertThat((String) ctx.inAppPayload().get("message")).isEqualTo("Minh vừa trả lời bài \"Ôn chương 3\" của bạn ở Lớp Thảo.");
        verify(idempotencyService).markSuccess("msg-8");
        verify(channel).basicAck(8L, false);
    }

    @Test
    void aRedeliveredMessageIsANoOp() throws Exception {
        when(idempotencyService.isProcessed("msg-4")).thenReturn(true);

        listener.handle(notice("u-1"), channel, 4L, "msg-4", "campus.event.scheduled");

        verify(dispatchService, never()).dispatch(any());
        verify(channel).basicAck(4L, false);
    }

    @Test
    void noRecipientsOrAnUnknownKeyIsAckedWithoutSending() throws Exception {
        listener.handle(notice(), channel, 5L, "msg-5", "campus.event.scheduled");
        listener.handle(notice("u-1"), channel, 6L, "msg-6", "campus.event.moved");

        verify(dispatchService, never()).dispatch(any());
        verify(channel).basicAck(5L, false);
        verify(channel).basicAck(6L, false);
    }

    @Test
    void aFailedDispatchIsDeadLetteredAndNotMarked() throws Exception {
        doThrow(new RuntimeException("db down")).when(dispatchService).dispatch(any());

        listener.handle(notice("u-1"), channel, 7L, "msg-7", "campus.event.reminder.due");

        verify(idempotencyService, never()).markSuccess(any());
        verify(channel).basicNack(7L, false, false);
    }

    private CampusEventNotice notice(String... recipients) {
        return CampusEventNotice.builder()
                .eventId("ev-1").zoneId("z-1").zoneTitle("Phòng thí nghiệm Dữ liệu").instructorId("ins-1")
                .title("Chữa bài SQL").kind("lecture").startsAt(STARTS).endsAt(STARTS.plusSeconds(3600))
                .recipientIds(Arrays.asList(recipients)).build();
    }

    private NotificationContext dispatched() {
        ArgumentCaptor<NotificationContext> captor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(dispatchService).dispatch(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<String> userIds(NotificationContext context) {
        return (List<String>) context.inAppPayload().get("userIds");
    }
}
