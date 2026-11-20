package com.cinx.notification.notification.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.notification.notification.application.command.SendNotificationCommand;
import com.cinx.notification.notification.application.query.NotificationCounts;
import com.cinx.notification.notification.application.query.NotificationQueries;
import com.cinx.notification.notification.application.query.TypeCount;
import com.cinx.notification.notification.domain.Notification;
import com.cinx.notification.notification.domain.NotificationRepository;
import com.cinx.notification.notification.domain.UserNotification;
import com.cinx.notification.notification.domain.UserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private UserNotificationRepository userNotificationRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationQueries notificationQueries;
    @Mock
    private RealtimeNotificationPublisher realtimePublisher;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(userNotificationRepository, notificationRepository, notificationQueries, realtimePublisher);
    }

    @Test
    void getNotificationsPassesTheFiltersAndTheRequestedPageAndSortToTheQueryPort() {
        PageResult<?> empty = new PageResult<>(List.of(), 3, 20, 0);
        when(notificationQueries.findPage(eq("user-1"), eq("java"), eq(false), eq("COURSE_PUBLISHED"), any(PageQuery.class)))
                .thenAnswer(call -> empty);

        // surrounding spaces of the type are trimmed
        service.getNotifications("user-1", "java", false, " COURSE_PUBLISHED ", 3, 20, "{\"sentAt\":\"ASC\"}");

        verify(notificationQueries).findPage("user-1", "java", false, "COURSE_PUBLISHED", new PageQuery(3, 20, "{\"sentAt\":\"ASC\"}"));
    }

    @Test
    void getNotificationsTreatsABlankTypeAsNoFilter() {
        service.getNotifications("user-1", null, null, "   ", 1, 10, null);

        verify(notificationQueries).findPage("user-1", null, null, null, new PageQuery(1, 10, null));
    }

    @Test
    void getCountsAddsEveryTypeIncludingUntypedOnesButListsOnlyTypedOnes() {
        // 5 + 2 + 1 = 8 in all; 3 + 0 + 1 = 4 unread; the untyped row counts in the totals but has no entry per type
        when(notificationQueries.countsByType("user-1")).thenReturn(List.of(
                new TypeCount("COURSE_PUBLISHED", 5, 3),
                new TypeCount("PAYMENT_PAID", 2, 0),
                new TypeCount(null, 1, 1)));

        NotificationCounts counts = service.getCounts("user-1");

        assertThat(counts.total()).isEqualTo(8);
        assertThat(counts.unread()).isEqualTo(4);
        assertThat(counts.byType()).containsExactly(Map.entry("COURSE_PUBLISHED", 5L), Map.entry("PAYMENT_PAID", 2L));
    }

    @Test
    void countUnreadComesFromTheQueryPort() {
        when(notificationQueries.countUnread("user-1")).thenReturn(7L);

        assertThat(service.countUnreadNotifications("user-1")).isEqualTo(7L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendNotificationPersistsSentAtForEachRecipient() {
        LocalDateTime sentAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        Notification notification = Notification.builder().id("notification-1").build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        service.sendNotification(new SendNotificationCommand(
                "Title",
                "Message",
                "COURSE_QUESTION_CREATED",
                "question-1",
                "/courses/course-1/questions/question-1",
                Map.of("courseId", "course-1"),
                List.of("user-1", "user-2")
        ), sentAt);

        ArgumentCaptor<List<UserNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(userNotificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .hasSize(2)
                .allSatisfy(userNotification -> {
                    assertThat(userNotification.getNotificationId()).isEqualTo("notification-1");
                    assertThat(userNotification.getSentAt()).isEqualTo(sentAt);
                    assertThat(userNotification.isRead()).isFalse();
                });
        assertThat(captor.getValue()).extracting(UserNotification::getUserId).containsExactly("user-1", "user-2");
    }

    @Test
    void sendNotificationStoresTheContentOnceWithItsMetadata() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(Notification.builder().id("notification-1").build());

        service.sendNotification(new SendNotificationCommand(
                "Title", "Message", "COURSE_QUESTION_CREATED", "question-1", "/courses/course-1",
                Map.of("courseId", "course-1"), List.of("user-1", "user-2")), LocalDateTime.of(2025, 1, 1, 10, 0));

        ArgumentCaptor<Notification> stored = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(stored.capture());
        assertThat(stored.getValue().getTitle()).isEqualTo("Title");
        assertThat(stored.getValue().getType()).isEqualTo("COURSE_QUESTION_CREATED");
        assertThat(stored.getValue().getReferenceId()).isEqualTo("question-1");
        assertThat(stored.getValue().getActionUrl()).isEqualTo("/courses/course-1");
        assertThat(stored.getValue().getMetadata()).containsEntry("courseId", "course-1");
    }

    @Test
    void sendInAppStoresTheNotificationAndPushesItToEveryRecipient() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(Notification.builder().id("notification-1").build());

        service.sendInApp(new SendNotificationCommand("Title", "Message", "PAYMENT_SUCCEEDED", "order-1", "/orders/order-1",
                Map.of("orderId", "order-1"), List.of("user-1", "user-2")));

        verify(userNotificationRepository).saveAll(any());
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(realtimePublisher).publish(eq("user-1"), payload.capture());
        verify(realtimePublisher).publish(eq("user-2"), any());
        assertThat(payload.getValue()).containsEntry("title", "Title").containsEntry("type", "PAYMENT_SUCCEEDED")
                .containsEntry("referenceId", "order-1").containsEntry("actionUrl", "/orders/order-1")
                .containsKey("sentAt").containsKey("metadata");
    }

    @Test
    void sendInAppPushesEmptyStringsAndAnEmptyMapForMissingOptionalFields() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(Notification.builder().id("notification-1").build());

        service.sendInApp(new SendNotificationCommand("Title", "Message", null, null, null, null, List.of("user-1")));

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(realtimePublisher).publish(eq("user-1"), payload.capture());
        assertThat(payload.getValue()).containsEntry("type", "").containsEntry("referenceId", "").containsEntry("actionUrl", "");
        assertThat((Map<?, ?>) payload.getValue().get("metadata")).isEmpty();
    }

    @Test
    void sendNotificationAloneNeverPushesInRealTime() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(Notification.builder().id("notification-1").build());

        service.sendNotification(new SendNotificationCommand("Title", "Message", null, null, null, null, List.of("user-1")));

        verify(realtimePublisher, never()).publish(any(), any());
    }

    @Test
    void toggleReadFlipsTheStateAndSavesIt() {
        UserNotification unread = UserNotification.unread("user-1", "notification-1", LocalDateTime.of(2025, 1, 1, 10, 0));
        when(userNotificationRepository.findByIdAndUserId("row-1", "user-1")).thenReturn(Optional.of(unread));

        service.toggleRead("user-1", "row-1");

        verify(userNotificationRepository).save(unread);
        assertThat(unread.isRead()).isTrue();
    }

    @Test
    void toggleReadOfSomeoneElsesRowIsNotFound() {
        when(userNotificationRepository.findByIdAndUserId("row-1", "intruder")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleRead("intruder", "row-1")).isInstanceOf(NotFoundException.class);
        verify(userNotificationRepository, never()).save(any());
    }

    @Test
    void markAllAsReadAndUnreadSetTheFlagForTheUser() {
        service.markAllAsRead("user-1");
        service.markAllAsUnread("user-1");

        verify(userNotificationRepository).setReadForAll("user-1", true);
        verify(userNotificationRepository).setReadForAll("user-1", false);
    }

    @Test
    void deleteOfARowThatIsNotTheUsersIsNotFound() {
        when(userNotificationRepository.deleteByIdAndUserId("row-1", "user-1")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteNotification("user-1", "row-1")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteOfTheUsersOwnRowSucceeds() {
        when(userNotificationRepository.deleteByIdAndUserId("row-1", "user-1")).thenReturn(true);

        service.deleteNotification("user-1", "row-1");

        verify(userNotificationRepository).deleteByIdAndUserId("row-1", "user-1");
    }
}
