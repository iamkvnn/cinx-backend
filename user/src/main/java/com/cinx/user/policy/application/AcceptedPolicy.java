package com.cinx.user.policy.application;

import com.cinx.user.policy.domain.PolicyType;

/** A published policy a learner agreed to, with the version number in force. */
public record AcceptedPolicy(String policyId, Integer versionNumber, PolicyType policyType) {
}
