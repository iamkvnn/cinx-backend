package com.cinx.learning.community.application;

import com.cinx.learning.community.application.event.CourseReviewCreatedEvent;

/** Port: records the review event in the caller's transaction, for the outbox relay. */
public interface CourseReviewEvents {
    void publishReviewCreatedEvent(CourseReviewCreatedEvent event);
}
