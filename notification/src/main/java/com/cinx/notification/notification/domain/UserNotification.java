package com.cinx.notification.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** One recipient's copy of a notification: only its read state changes after it is sent. */
@Getter
@Builder
@AllArgsConstructor
public class UserNotification {
    private final String id;
    private final String userId;
    private final String notificationId;
    private boolean read;
    private final LocalDateTime sentAt;

    public static UserNotification unread(String userId, String notificationId, LocalDateTime sentAt) {
        return UserNotification.builder().userId(userId).notificationId(notificationId).read(false).sentAt(sentAt).build();
    }

    public void toggleRead() {
        read = !read;
    }
}
