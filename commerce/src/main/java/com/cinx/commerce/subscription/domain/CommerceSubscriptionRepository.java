package com.cinx.commerce.subscription.domain;

import java.util.List;
import java.util.Optional;

/** Port: where subscriptions are stored. */
public interface CommerceSubscriptionRepository {
    Optional<CommerceSubscription> findById(String id);

    /** The subject's subscriptions, the newest first. */
    List<CommerceSubscription> findAllBySubjectId(String subjectId);

    /** Up to 100 subscriptions the billing provider knows, the least recently updated first. */
    List<CommerceSubscription> findReconcilable();

    Optional<CommerceSubscription> findByProviderRef(String provider, String providerAccountKey, String providerSubscriptionRef);

    /** Stores the subscription and writes it out at once. */
    CommerceSubscription save(CommerceSubscription subscription);
}
