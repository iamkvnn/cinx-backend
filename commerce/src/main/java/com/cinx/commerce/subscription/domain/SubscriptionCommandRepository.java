package com.cinx.commerce.subscription.domain;

import java.util.Optional;

/** Port: where the idempotent commands of a subject are stored. */
public interface SubscriptionCommandRepository {
    Optional<SubscriptionCommand> findBySubjectIdAndIdempotencyKey(String subjectId, String idempotencyKey);

    /** Stores the command, or returns nothing when the subject already has a command with that idempotency key. */
    Optional<SubscriptionCommand> insertIfAbsent(SubscriptionCommand command);

    SubscriptionCommand save(SubscriptionCommand command);
}
