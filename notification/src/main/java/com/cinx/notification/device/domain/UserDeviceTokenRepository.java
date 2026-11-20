package com.cinx.notification.device.domain;

import java.util.List;
import java.util.Optional;

/** Port: where device tokens are stored. */
public interface UserDeviceTokenRepository {
    Optional<UserDeviceToken> findByFcmToken(String fcmToken);

    List<UserDeviceToken> findByUserId(String userId);

    UserDeviceToken save(UserDeviceToken token);
}
