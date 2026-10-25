package com.cinx.learning.catalog.domain;

import java.util.Optional;

/** Port: where CourseAccessPolicy rows are stored. */
public interface CourseAccessPolicyRepository {
    void delete(CourseAccessPolicy entity);

    Optional<CourseAccessPolicy> findById(String id);

    CourseAccessPolicy save(CourseAccessPolicy entity);
}
