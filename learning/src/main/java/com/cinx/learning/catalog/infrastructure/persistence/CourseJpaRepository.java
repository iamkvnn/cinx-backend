package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.shared.domain.CourseMembershipStatus;

import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, String> {
    @Query("""
        SELECT c
        FROM Course c
        LEFT JOIN c.category cat
        WHERE
            (:query IS NULL OR
                c.title LIKE %:query%
                OR c.description LIKE %:query%)
            AND (:categoryId IS NULL OR cat.id = :categoryId)
            AND (:instructorId IS NULL OR c.instructorId = :instructorId
                OR EXISTS (
                    SELECT m.id FROM CourseInstructorMembership m
                    WHERE m.courseId = c.id
                      AND m.instructorId = :instructorId
                      AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
                ))
            AND (:rating IS NULL OR c.rating >= :rating)
            AND (:priceFrom IS NULL OR COALESCE(c.discountedPrice, c.price) >= :priceFrom)
            AND (:priceTo IS NULL OR COALESCE(c.discountedPrice, c.price) <= :priceTo)
            AND (:hasCertificate IS NULL OR c.hasCertificate = :hasCertificate)
            AND (:onSale IS NULL OR (:onSale = TRUE AND c.discountRate > 0) OR (:onSale = FALSE AND (c.discountRate IS NULL OR c.discountRate = 0)))
            AND (:status IS NULL OR c.status = :status)
            AND (:publishStatus IS NULL OR c.publishStatus = :publishStatus)
    """)
    Page<CourseJpaEntity> searchAll(
            @Param("query") String query,
            @Param("categoryId") String categoryId,
            @Param("instructorId") String instructorId,
            @Param("rating") Integer rating,
            @Param("priceFrom") BigDecimal priceFrom,
            @Param("priceTo") BigDecimal priceTo,
            @Param("hasCertificate") Boolean hasCertificate,
            @Param("onSale") Boolean onSale,
            @Param("status") CourseStatus status,
            @Param("publishStatus") CoursePublishStatus publishStatus,
            Pageable pageable
    );

    @Query("""
        SELECT c
        FROM Course c
        LEFT JOIN c.category cat
        WHERE c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
            AND (:query IS NULL OR
                c.title LIKE %:query%
                OR c.description LIKE %:query%)
            AND (:categoryId IS NULL OR cat.id = :categoryId)
            AND (:instructorId IS NULL OR c.instructorId = :instructorId
                OR EXISTS (
                    SELECT m.id FROM CourseInstructorMembership m
                    WHERE m.courseId = c.id
                      AND m.instructorId = :instructorId
                      AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
                ))
            AND (:rating IS NULL OR c.rating >= :rating)
            AND (:priceFrom IS NULL OR COALESCE(c.discountedPrice, c.price) >= :priceFrom)
            AND (:priceTo IS NULL OR COALESCE(c.discountedPrice, c.price) <= :priceTo)
            AND (:hasCertificate IS NULL OR c.hasCertificate = :hasCertificate)
            AND (:onSale IS NULL OR (:onSale = TRUE AND c.discountRate > 0) OR (:onSale = FALSE AND (c.discountRate IS NULL OR c.discountRate = 0)))
            AND (:durationFrom IS NULL OR c.duration >= :durationFrom)
            AND (:durationTo IS NULL OR c.duration < :durationTo)
    """)
    Page<CourseJpaEntity> searchPublished(
            @Param("query") String query,
            @Param("categoryId") String categoryId,
            @Param("instructorId") String instructorId,
            @Param("rating") Integer rating,
            @Param("priceFrom") BigDecimal priceFrom,
            @Param("priceTo") BigDecimal priceTo,
            @Param("hasCertificate") Boolean hasCertificate,
            @Param("onSale") Boolean onSale,
            @Param("durationFrom") Long durationFrom,
            @Param("durationTo") Long durationTo,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(DISTINCT c.instructorId)
        FROM Course c
        WHERE c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
    """)
    long countDistinctInstructorsOfPublishedCourses();

    @Query("""
        SELECT c.category.id, COUNT(c)
        FROM Course c
        WHERE c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
          AND c.category IS NOT NULL
        GROUP BY c.category.id
    """)
    List<Object[]> countPublishedByCategory();

    @Query("""
        SELECT c
        FROM Course c
        WHERE c.id IN :ids
          AND c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
    """)
    List<CourseJpaEntity> findPublishedByIds(@Param("ids") List<String> ids);

    @Query("""
        SELECT c
        FROM Course c
        WHERE c.id IN :ids
            AND c.status IN (com.cinx.learning.shared.domain.CourseStatus.PUBLISHED, com.cinx.learning.shared.domain.CourseStatus.ARCHIVED)
    """)
    List<CourseJpaEntity> findEnrolledReadableByIds(@Param("ids") List<String> ids);

    @Query("""
        SELECT c.id
        FROM Course c
        WHERE c.id IN :ids
            AND c.status IN (com.cinx.learning.shared.domain.CourseStatus.PUBLISHED, com.cinx.learning.shared.domain.CourseStatus.ARCHIVED)
            AND (:query IS NULL OR c.id LIKE %:query% OR c.title LIKE %:query% OR c.description LIKE %:query%)
    """)
    List<String> searchEnrolledReadableIds(@Param("ids") List<String> ids, @Param("query") String query);

    @Query("""
        SELECT COUNT(c)
        FROM Course c
        WHERE c.instructorId = :instructorId
           OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
           )
    """)
    long countByInstructorId(@Param("instructorId") String instructorId);

    @Query("""
        SELECT COUNT(c)
        FROM Course c
        WHERE c.status = :status
          AND (c.instructorId = :instructorId OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
          ))
    """)
    long countByInstructorIdAndStatus(@Param("instructorId") String instructorId,
                                      @Param("status") CourseStatus status);

    @Query("""
        SELECT c.id
        FROM Course c
        WHERE c.instructorId = :instructorId
           OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
           )
    """)
    List<String> findIdsByInstructorId(@Param("instructorId") String instructorId);

    long countByStatus(CourseStatus status);

    @Query("""
        SELECT AVG(c.rating)
        FROM Course c
        WHERE c.rating IS NOT NULL
          AND (c.instructorId = :instructorId OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
          ))
    """)
    Double averageRatingByInstructorId(@Param("instructorId") String instructorId);

    @Query("""
        SELECT COALESCE(SUM(c.enrollmentCount), 0)
        FROM Course c
        WHERE c.instructorId = :instructorId
           OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
           )
    """)
    Long sumEnrollmentCountByInstructorId(@Param("instructorId") String instructorId);

    /** Public figures of one instructor, over PUBLISHED courses they own or are an ACTIVE member of. */
    interface InstructorPublishedStats {
        Long getCourseCount();

        Long getEnrollmentCount();

        Long getRatingCount();

        /** Rating votes on courses that have a rating: the denominator of the weighted average. */
        Long getRatedCount();

        /** Sum of rating × ratingCount, so that dividing by {@link #getRatedCount()} weights each course by its votes. */
        Double getWeightedRatingSum();
    }

    @Query("""
        SELECT COUNT(c) AS courseCount,
               COALESCE(SUM(c.enrollmentCount), 0) AS enrollmentCount,
               COALESCE(SUM(c.ratingCount), 0) AS ratingCount,
               COALESCE(SUM(CASE WHEN c.rating IS NOT NULL THEN c.ratingCount ELSE 0L END), 0L) AS ratedCount,
               COALESCE(SUM(CASE WHEN c.rating IS NOT NULL THEN c.rating * c.ratingCount ELSE 0.0 END), 0.0) AS weightedRatingSum
        FROM Course c
        WHERE c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
          AND (c.instructorId = :instructorId OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
          ))
    """)
    InstructorPublishedStats summarizePublishedByInstructor(@Param("instructorId") String instructorId);

    /** Rows of {category id, category name, course count}, most courses first. */
    @Query("""
        SELECT cat.id, cat.name, COUNT(c)
        FROM Course c
        JOIN c.category cat
        WHERE c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
          AND (c.instructorId = :instructorId OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
          ))
        GROUP BY cat.id, cat.name
        ORDER BY COUNT(c) DESC, cat.name ASC
    """)
    List<Object[]> countPublishedByCategoryForInstructor(@Param("instructorId") String instructorId);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.createdAt BETWEEN :start AND :end")
    long countCreatedCoursesBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT COUNT(c)
        FROM Course c
        WHERE c.createdAt BETWEEN :start AND :end
          AND (c.instructorId = :instructorId OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
          ))
    """)
    long countCreatedCoursesByInstructorBetween(@Param("instructorId") String instructorId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("""
        SELECT c.status, COUNT(c)
        FROM Course c
        WHERE c.createdAt BETWEEN :start AND :end
        GROUP BY c.status
    """)
    List<Object[]> countCreatedCoursesByStatusBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT c.status, COUNT(c)
        FROM Course c
        GROUP BY c.status
    """)
    List<Object[]> countCurrentCoursesByStatus();

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m-%d'), COUNT(c)
        FROM Course c
        WHERE c.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m-%d')
        ORDER BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m-%d') ASC
    """)
    List<Object[]> aggregateCreatedCoursesByDay(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m'), COUNT(c)
        FROM Course c
        WHERE c.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m') ASC
    """)
    List<Object[]> aggregateCreatedCoursesByMonth(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m-%d'), COUNT(c)
        FROM Course c
        WHERE c.createdAt BETWEEN :start AND :end
            AND (c.instructorId = :instructorId OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
            ))
        GROUP BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m-%d')
        ORDER BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m-%d') ASC
    """)
    List<Object[]> aggregateCreatedCoursesByInstructorAndDay(String instructorId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m'), COUNT(c)
        FROM Course c
        WHERE c.createdAt BETWEEN :start AND :end
            AND (c.instructorId = :instructorId OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE m.courseId = c.id
                  AND m.instructorId = :instructorId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
            ))
        GROUP BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m') ASC
    """)
    List<Object[]> aggregateCreatedCoursesByInstructorAndMonth(String instructorId, LocalDateTime start, LocalDateTime end);
}

