package com.cinx.learning.progress.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where CourseProgress rows are stored. */
public interface CourseProgressRepository {
    List<CourseProgress> findAllByUserId(String userId);

    List<CourseProgress> findAllByUserIdAndCourseIdIn(String userId, List<String> courseIds);

    Optional<CourseProgress> findByUserIdAndCourseId(String userId, String courseId);

    boolean existsByUserIdAndCourseId(String userId, String courseId);

    List<CourseProgress> findAllByCourseId(String courseId);

    List<CourseProgress> findAllByCourseIdIn(List<String> courseIds);

    /** Learners with progress in each course, and how many of them completed it. */
    List<CompletionRow> completionPerCourse(java.util.Collection<String> courseIds);

    CourseProgress save(CourseProgress entity);

    interface CompletionRow {
            String getCourseId();
            long getTotal();
            Long getCompleted();
        }

    Optional<CourseProgress> findById(String id);
}
