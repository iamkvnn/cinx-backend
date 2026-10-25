package com.cinx.learning.catalog.domain;

/** How a course is opened; there is no free model, every course is sold. */
public enum CourseAccessModel {
    PURCHASE_ONLY,
    SUBSCRIPTION_ONLY,
    PURCHASE_OR_SUBSCRIPTION
}
