package com.cinx.notification.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotificationTest {
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2025, 1, 1, 10, 0);

    @Test
    void aFreshNotificationIsUnreadAndKeepsItsRecipientAndTime() {
        UserNotification notification = UserNotification.unread("user-1", "notification-1", SENT_AT);

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getUserId()).isEqualTo("user-1");
        assertThat(notification.getNotificationId()).isEqualTo("notification-1");
        assertThat(notification.getSentAt()).isEqualTo(SENT_AT);
        assertThat(notification.getId()).isNull();
    }

    @Test
    void toggleReadFlipsInBothDirections() {
        UserNotification notification = UserNotification.unread("user-1", "notification-1", SENT_AT);

        notification.toggleRead();
        assertThat(notification.isRead()).isTrue();

        notification.toggleRead();
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void toggleReadLeavesEverythingElseAlone() {
        UserNotification notification = UserNotification.builder().id("row-1").userId("user-1")
                .notificationId("notification-1").read(true).sentAt(SENT_AT).build();

        notification.toggleRead();

        assertThat(notification.getId()).isEqualTo("row-1");
        assertThat(notification.getUserId()).isEqualTo("user-1");
        assertThat(notification.getNotificationId()).isEqualTo("notification-1");
        assertThat(notification.getSentAt()).isEqualTo(SENT_AT);
        assertThat(notification.isRead()).isFalse();
    }
}
