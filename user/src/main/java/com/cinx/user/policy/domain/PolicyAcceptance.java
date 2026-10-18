package com.cinx.user.policy.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** A user's agreement to one policy document, with the version number in force at that moment. */
@Getter
@Builder
@AllArgsConstructor
public class PolicyAcceptance {
    private final String id;
    private final String userId;
    private final String policyId;
    private final Integer policyVersion;
    private final LocalDateTime acceptedAt;
}
