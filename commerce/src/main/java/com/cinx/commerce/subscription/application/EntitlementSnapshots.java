package com.cinx.commerce.subscription.application;

import java.util.Map;

/** Port: tells the other services what a subject is now entitled to, through the outbox in the caller's transaction. */
public interface EntitlementSnapshots {
    void publish(String subjectId, Map<String, Object> snapshot);
}
