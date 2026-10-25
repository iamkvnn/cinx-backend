package com.cinx.learning.catalog.domain;

import java.util.Optional;

/** Port: where CourseDraft rows are stored. */
public interface CourseDraftRepository {
    Optional<CourseDraft> findByCourse(String courseId);

    void delete(CourseDraft entity);

    CourseDraft save(CourseDraft entity);
}
