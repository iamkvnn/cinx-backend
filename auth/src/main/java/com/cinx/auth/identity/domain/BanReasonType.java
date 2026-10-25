package com.cinx.auth.identity.domain;

import lombok.Getter;

@Getter
public enum BanReasonType {
    SPAM(7),
    NEGATIVE_WORDS(7),
    INSULT(365),
    POLICY_ABUSE(null);

    private final Integer maxDurationDays;

    BanReasonType(Integer maxDurationDays) {
        this.maxDurationDays = maxDurationDays;
    }
}
