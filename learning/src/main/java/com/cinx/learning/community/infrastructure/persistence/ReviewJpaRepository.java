package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, String> {
    Page<ReviewJpaEntity> findByCourseId(String courseId, Pageable pageable);

    /** Reviews of a course its instructors have not replied to yet. */
    @Query("""
        SELECT r FROM Review r
        WHERE r.courseId = :courseId
          AND NOT EXISTS (SELECT rr.id FROM ReviewReply rr WHERE rr.reviewId = r.id)
    """)
    Page<ReviewJpaEntity> findUnrepliedByCourseId(@Param("courseId") String courseId, Pageable pageable);

    /** Unreplied reviews of these courses: count and the oldest one, per course. */
    @Query("""
        SELECT r.courseId AS courseId, COUNT(r) AS waiting, MIN(r.createdAt) AS oldest
        FROM Review r
        WHERE r.courseId IN :courseIds
          AND NOT EXISTS (SELECT rr.id FROM ReviewReply rr WHERE rr.reviewId = r.id)
        GROUP BY r.courseId
    """)
    List<UnrepliedGroup> findUnrepliedGroups(@Param("courseIds") java.util.Collection<String> courseIds);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.courseId IN :courseIds")
    Double averageRating(@Param("courseIds") java.util.Collection<String> courseIds);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.courseId IN :courseIds AND r.createdAt < :before")
    Double averageRatingBefore(@Param("courseIds") java.util.Collection<String> courseIds, @Param("before") java.time.LocalDateTime before);

    long countByCourseIdIn(java.util.Collection<String> courseIds);

    interface UnrepliedGroup {
        String getCourseId();
        long getWaiting();
        java.time.LocalDateTime getOldest();
    }

    boolean existsByUserIdAndCourseId(String userId, String courseId);

    Optional<ReviewJpaEntity> findByUserIdAndCourseId(String userId, String courseId);

    Long countByCourseId(String courseId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.courseId = :courseId")
    Double getAverageRatingByCourseId(@Param("courseId") String courseId);

    @Query("""
        SELECT r.rating, COUNT(r)
        FROM Review r
        WHERE r.courseId = :courseId
        GROUP BY r.rating
        ORDER BY r.rating ASC
    """)
    List<Object[]> countRatingsByCourseId(String courseId);
}
