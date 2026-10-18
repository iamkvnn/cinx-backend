package com.cinx.user.policy.application;

import java.time.LocalDateTime;
import java.util.List;

/** What other features may ask of the policy feature when a learner agrees to policies. */
public interface PolicyAcceptances {
    /**
     * The published policies the learner agreed to. Every id must be published, and when a TERMS or PRIVACY policy is
     * published the learner must have agreed to at least one of that type.
     */
    List<AcceptedPolicy> resolveAccepted(List<String> requestedIds);

    void record(String userId, List<AcceptedPolicy> accepted, LocalDateTime acceptedAt);
}
