package com.cinx.learning.progress.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CourseProgressJpaRepository extends JpaRepository<CourseProgressJpaEntity, String> {
    List<CourseProgressJpaEntity> findAllByUserId(String userId);

    List<CourseProgressJpaEntity> findAllByUserIdAndCourseIdIn(String userId, List<String> courseIds);

    Optional<CourseProgressJpaEntity> findByUserIdAndCourseId(String userId, String courseId);

    boolean existsByUserIdAndCourseId(String userId, String courseId);

    List<CourseProgressJpaEntity> findAllByCourseId(String courseId);

    List<CourseProgressJpaEntity> findAllByCourseIdIn(List<String> courseIds);

    /** Learners with progress in each course, and how many of them completed it. */
    @org.springframework.data.jpa.repository.Query("""
        select p.courseId as courseId, count(p) as total,
               sum(case when p.isCompleted = true then 1 else 0 end) as completed
        from CourseProgress p
        where p.courseId in :courseIds
        group by p.courseId
    """)
    List<CompletionRow> completionPerCourse(java.util.Collection<String> courseIds);

    interface CompletionRow {
        String getCourseId();
        long getTotal();
        Long getCompleted();
    }
}
