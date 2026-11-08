package com.cinx.commerce.subscription.domain;

import java.util.Optional;

/** Port: where the pointers to each subject's current subscriptions are stored. */
public interface CurrentSubscriptionPointerRepository {
    Optional<CurrentSubscriptionPointer> find(String subjectId, String slotCode);

    /** The pointer with a row lock held until the surrounding transaction ends. */
    Optional<CurrentSubscriptionPointer> lock(String subjectId, String slotCode);

    CurrentSubscriptionPointer save(CurrentSubscriptionPointer pointer);
}
