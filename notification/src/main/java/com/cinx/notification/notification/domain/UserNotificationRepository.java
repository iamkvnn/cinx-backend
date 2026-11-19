package com.cinx.notification.notification.domain;

import java.util.List;
import java.util.Optional;

/** Port: where each recipient's notifications are stored. Reads for lists and counts are in the application's query port. */
public interface UserNotificationRepository {
    List<UserNotification> saveAll(List<UserNotification> userNotifications);

    UserNotification save(UserNotification userNotification);

    /** Only the owner's row; another user's id is simply absent. */
    Optional<UserNotification> findByIdAndUserId(String id, String userId);

    /** @return whether a row of that owner was deleted */
    boolean deleteByIdAndUserId(String id, String userId);

    /** Sets the read state of every notification the user has. */
    void setReadForAll(String userId, boolean read);
}
