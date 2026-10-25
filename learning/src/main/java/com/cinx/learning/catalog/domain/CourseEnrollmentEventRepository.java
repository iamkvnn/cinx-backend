package com.cinx.learning.catalog.domain;


/** Port: where CourseEnrollmentEvent rows are stored. */
public interface CourseEnrollmentEventRepository {
    CourseEnrollmentEvent saveAndFlush(CourseEnrollmentEvent entity);
}
