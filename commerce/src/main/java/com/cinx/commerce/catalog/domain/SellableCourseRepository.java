package com.cinx.commerce.catalog.domain;

import java.util.Optional;

/** Port: the local copy of the learning service's courses, as far as selling them is concerned. */
public interface SellableCourseRepository {
    Optional<SellableCourse> findById(String courseId);

    SellableCourse save(SellableCourse course);
}
