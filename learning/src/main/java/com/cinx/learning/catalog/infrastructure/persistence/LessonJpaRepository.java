package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import com.cinx.learning.shared.domain.CourseStatus;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface LessonJpaRepository extends JpaRepository<LessonJpaEntity, String> {
    /** The readable (published or archived) lessons with these stable ids, with section and course fetched. */
    @Query("""
        SELECT l FROM Lesson l
        JOIN FETCH l.section s
        JOIN FETCH s.course c
        WHERE l.stableId IN :stableIds
            AND c.status IN (com.cinx.learning.shared.domain.CourseStatus.PUBLISHED, com.cinx.learning.shared.domain.CourseStatus.ARCHIVED)
    """)
    List<LessonJpaEntity> findReadableByStableIdIn(@Param("stableIds") java.util.Collection<String> stableIds);

    @Query("""
        SELECT l
        FROM Lesson l
        JOIN l.section s
        WHERE s.id = :sectionId
            AND l.stableId = :stableId
    """)
    Optional<LessonJpaEntity> findBySectionAndStableId(@Param("sectionId") String sectionId, @Param("stableId") String stableId);

    @Query("""
        SELECT l
        FROM Lesson l
        JOIN l.section s
        LEFT JOIN s.course c
        LEFT JOIN s.draft d
        LEFT JOIN d.course dc
        WHERE l.stableId = :stableId
            AND (c.id = :courseId OR dc.id = :courseId)
    """)
    List<LessonJpaEntity> findByCourseAndStableId(@Param("courseId") String courseId, @Param("stableId") String stableId);

    @Query("""
        SELECT l
        FROM Lesson l
        JOIN l.section s
        JOIN s.course c
        WHERE c.id = :courseId
            AND l.stableId = :stableId
            AND c.status IN (com.cinx.learning.shared.domain.CourseStatus.PUBLISHED, com.cinx.learning.shared.domain.CourseStatus.ARCHIVED)
    """)
    List<LessonJpaEntity> findReadableByCourseAndStableId(@Param("courseId") String courseId, @Param("stableId") String stableId);

    @Query("""
        SELECT l FROM Lesson l
        JOIN l.section s
        JOIN s.course c
        WHERE c.id = :courseId
            AND c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
        ORDER BY s.orderIndex ASC, s.stableId ASC, l.orderIndex ASC, l.stableId ASC
    """)
    List<LessonJpaEntity> findPublishedByCourse(@Param("courseId") String courseId);

    @Query("""
        SELECT DISTINCT c.id FROM Lesson l
        JOIN l.section s
        JOIN s.course c
        WHERE l.stableId = :stableId
            AND c.status = com.cinx.learning.shared.domain.CourseStatus.PUBLISHED
    """)
    List<String> findPublishedCourseIdsByStableId(@Param("stableId") String stableId);

    @Query("""
        SELECT new com.cinx.learning.catalog.domain.LessonPlacement(COALESCE(c.id, dc.id), l.title)
        FROM Lesson l
        JOIN l.section s
        LEFT JOIN s.course c
        LEFT JOIN s.draft d
        LEFT JOIN d.course dc
        WHERE l.stableId = :stableId
    """)
    List<com.cinx.learning.catalog.domain.LessonPlacement> findPlacementsByStableId(@Param("stableId") String stableId);

    @Query("""
        SELECT l FROM Lesson l
        JOIN l.section s
        JOIN s.course c
        WHERE c.id = :courseId
            AND c.status IN (com.cinx.learning.shared.domain.CourseStatus.PUBLISHED, com.cinx.learning.shared.domain.CourseStatus.ARCHIVED)
        ORDER BY s.orderIndex ASC, s.stableId ASC, l.orderIndex ASC, l.stableId ASC
    """)
    List<LessonJpaEntity> findEnrolledReadableByCourse(@Param("courseId") String courseId);

    /** Readable lessons of several courses, sections and courses fetched, in curriculum order within each course. */
    @Query("""
        SELECT l FROM Lesson l
        JOIN FETCH l.section s
        JOIN FETCH s.course c
        WHERE c.id IN :courseIds
            AND c.status IN (com.cinx.learning.shared.domain.CourseStatus.PUBLISHED, com.cinx.learning.shared.domain.CourseStatus.ARCHIVED)
        ORDER BY c.id ASC, s.orderIndex ASC, s.stableId ASC, l.orderIndex ASC, l.stableId ASC
    """)
    List<LessonJpaEntity> findEnrolledReadableByCourseIn(@Param("courseIds") java.util.Collection<String> courseIds);

    @Query("""
        SELECT l
        FROM Lesson l
        JOIN l.section s
        JOIN s.draft d
        WHERE d.id = :draftId
        ORDER BY s.orderIndex ASC, s.stableId ASC, l.orderIndex ASC, l.stableId ASC
    """)
    List<LessonJpaEntity> findByDraft(@Param("draftId") String draftId);

    @Query("""
        SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
        FROM Lesson l
        JOIN l.section s
        JOIN s.draft d
        WHERE d.id = :draftId
    """)
    boolean existsByDraft(@Param("draftId") String draftId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l
        FROM Lesson l
        JOIN l.section s
        WHERE s.id IN :sectionIds
        ORDER BY s.orderIndex ASC, s.stableId ASC, l.orderIndex ASC, l.stableId ASC
    """)
    List<LessonJpaEntity> findBySectionIdsForUpdate(@Param("sectionIds") List<String> sectionIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l
        FROM Lesson l
        JOIN FETCH l.section s
        JOIN s.draft d
        WHERE d.id = :draftId
            AND l.stableId = :stableId
    """)
    Optional<LessonJpaEntity> findDraftLessonForUpdate(@Param("draftId") String draftId, @Param("stableId") String stableId);

    @Query("""
        SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
        FROM Lesson l
        JOIN l.section s
        LEFT JOIN s.course c
        LEFT JOIN s.draft d
        LEFT JOIN d.course dc
        WHERE l.stableId = :lessonId
        AND (
            c.instructorId = :userId
            OR dc.instructorId = :userId
            OR EXISTS (
                SELECT m.id FROM CourseInstructorMembership m
                WHERE (m.courseId = c.id OR m.courseId = dc.id)
                  AND m.instructorId = :userId
                  AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
                  AND m.role IN (com.cinx.learning.shared.domain.CourseMembershipRole.OWNER,
                                 com.cinx.learning.shared.domain.CourseMembershipRole.MANAGER)
            )
        )
    """)
    boolean isAccessibleByInstructor(@Param("lessonId") String lessonId, @Param("userId") String userId);

    @Query("""
        SELECT l FROM Lesson l
        JOIN l.section s
        JOIN s.draft d
        WHERE d.id = :draftId
        ORDER BY s.orderIndex ASC, s.stableId ASC, l.orderIndex ASC, l.stableId ASC
    """)
    List<LessonJpaEntity> findDraftByDraft(@Param("draftId") String draftId);
}

