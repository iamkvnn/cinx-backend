package com.cinx.user.profile.application.command;

import com.cinx.user.profile.domain.AccountType;
import com.cinx.user.profile.domain.Gender;

/** Creates a profile directly (a service-to-service call); an instructor account starts with an application under review. */
public record CreateUserCommand(
        String userId,
        String name,
        String email,
        AccountType accountType,
        Gender gender,
        String phoneNumber,
        String bio,
        String cvFileKey) {
}
