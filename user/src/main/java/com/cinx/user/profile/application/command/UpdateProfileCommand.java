package com.cinx.user.profile.application.command;

import com.cinx.user.profile.domain.Gender;

/** The profile fields a person may change; a null field leaves the stored value as it is. */
public record UpdateProfileCommand(
        String name,
        Gender gender,
        String phoneNumber,
        String bio,
        Boolean isReceivePushNotification,
        String avatarFileKey,
        String cvFileKey) {
}
