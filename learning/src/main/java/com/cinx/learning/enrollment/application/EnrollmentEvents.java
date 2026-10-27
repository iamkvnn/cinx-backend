package com.cinx.learning.enrollment.application;

import com.cinx.learning.enrollment.domain.CourseEntitlement;

/** Port: records the enrollment events in the caller's transaction, for the outbox relay. */
public interface EnrollmentEvents {
    void publishGranted(CourseEntitlement entitlement);
}
