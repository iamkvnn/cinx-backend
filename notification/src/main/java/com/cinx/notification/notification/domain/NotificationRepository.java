package com.cinx.notification.notification.domain;

/** Port: where notification content is stored. */
public interface NotificationRepository {
    /** Stores the notification and returns it with its id assigned. */
    Notification save(Notification notification);
}
