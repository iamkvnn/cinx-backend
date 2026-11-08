package com.cinx.commerce.subscription.domain;

import java.util.Optional;

/** Port: where the subjects that can hold subscriptions (today, users) are stored. */
public interface SubscriptionSubjectRepository {
    Optional<SubscriptionSubject> findById(String id);

    Optional<SubscriptionSubject> findByTypeAndReference(String subjectType, String subjectReference);

    /** The subject with a row lock held until the surrounding transaction ends. */
    Optional<SubscriptionSubject> lockByTypeAndReference(String subjectType, String subjectReference);

    /** The stored subject of the candidate's type and reference, or the candidate stored now; safe when two callers race. */
    SubscriptionSubject findOrCreate(SubscriptionSubject candidate);

    /** Like {@link #findOrCreate} but the subject is returned locked until the surrounding transaction ends. */
    SubscriptionSubject lockOrCreate(SubscriptionSubject candidate);
}
