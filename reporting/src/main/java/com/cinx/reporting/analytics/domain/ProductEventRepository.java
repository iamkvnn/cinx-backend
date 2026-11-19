package com.cinx.reporting.analytics.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductEventRepository {
    /** Stores the events a user has not sent before; returns how many were new. */
    int insertNew(List<ProductEvent> events);

    /** Keeps the first exposure of a user to an experiment; returns whether this one was it. */
    boolean recordFirstExposure(String experimentKey, String userId, String variant, LocalDateTime exposedAt);

    /** Writes the events and distinct users per event name of the UTC range [from, to) as {@code day}. */
    void rollUpDay(LocalDate day, LocalDateTime from, LocalDateTime to);

    /** Deletes up to {@code limit} raw events older than {@code before}; returns how many went. */
    int deleteOlderThan(LocalDateTime before, int limit);
}
