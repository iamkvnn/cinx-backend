package com.cinx.auth.identity.application.command;

import com.cinx.auth.identity.domain.BanReasonType;

/** Suspends an account; a null {@code durationDays} means until someone reactivates it. */
public record BanUserCommand(BanReasonType reasonType, String details, Integer durationDays) {
}
