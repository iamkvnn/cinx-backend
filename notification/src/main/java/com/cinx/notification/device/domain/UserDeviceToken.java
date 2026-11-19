package com.cinx.notification.device.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** A push token of one device. A token belongs to whoever registered it last, so a shared device follows its current user. */
@Getter
@Builder
@AllArgsConstructor
public class UserDeviceToken {
    private final String id;
    private String userId;
    private final String fcmToken;
    private String deviceInfo;

    public static UserDeviceToken forToken(String fcmToken) {
        return UserDeviceToken.builder().fcmToken(fcmToken).build();
    }

    public void assignTo(String userId, String deviceInfo) {
        this.userId = userId;
        this.deviceInfo = deviceInfo;
    }
}
