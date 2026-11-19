package com.cinx.reporting.analytics.application;

/** The user sent more events this hour than {@link EventRateLimiter#PER_HOUR}; answered with 429. */
public class EventRateLimitedException extends RuntimeException {
    public EventRateLimitedException() {
        super("Too many product events this hour");
    }
}
