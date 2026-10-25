package com.cinx.learning.catalog.domain;

import java.util.List;
import java.util.Optional;

/** Port: where CourseImage rows are stored. */
public interface CourseImageRepository {
    void delete(CourseImage entity);

    Optional<CourseImage> findById(String id);

    CourseImage save(CourseImage entity);

    List<CourseImage> saveAll(Iterable<CourseImage> entities);
}
