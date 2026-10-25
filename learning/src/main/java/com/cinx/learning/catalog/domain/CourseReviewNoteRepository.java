package com.cinx.learning.catalog.domain;

import com.cinx.learning.shared.domain.ReviewNoteStatus;

import java.util.List;
import java.util.Optional;

/** Port for the stored CourseReviewNote rows. */
public interface CourseReviewNoteRepository {
    List<CourseReviewNote> findAllByCourseIdAndStatusOrderByCreatedAtAsc(String courseId, ReviewNoteStatus status);

    List<CourseReviewNote> findAllByCourseId(String courseId);

    void delete(CourseReviewNote entity);

    void deleteAll(Iterable<CourseReviewNote> entities);

    Optional<CourseReviewNote> findById(String id);

    CourseReviewNote save(CourseReviewNote entity);

    List<CourseReviewNote> saveAll(Iterable<CourseReviewNote> entities);
}
