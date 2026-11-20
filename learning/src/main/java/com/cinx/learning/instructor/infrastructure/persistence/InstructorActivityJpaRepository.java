package com.cinx.learning.instructor.infrastructure.persistence;

import com.cinx.learning.shared.domain.InboxKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface InstructorActivityJpaRepository extends JpaRepository<InstructorActivityJpaEntity, String> {
    List<InstructorActivityJpaEntity> findAllByCourseId(String courseId);

    @Query("""
        SELECT a FROM InstructorActivity a
        WHERE a.instructorId = :instructorId
            AND a.createdAt >= :since
            AND (:kind IS NULL OR a.kind = :kind)
            AND (:courseId IS NULL OR a.courseId = :courseId)
        ORDER BY a.createdAt DESC
    """)
    List<InstructorActivityJpaEntity> findRecent(@Param("instructorId") String instructorId,
                                        @Param("since") LocalDateTime since,
                                        @Param("kind") InboxKind kind,
                                        @Param("courseId") String courseId,
                                        Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(a.itemCount), 0) FROM InstructorActivity a
        WHERE a.instructorId = :instructorId AND a.createdAt >= :since
    """)
    long sumItemsSince(@Param("instructorId") String instructorId, @Param("since") LocalDateTime since);

    @Query("""
        SELECT COALESCE(SUM(a.itemCount), 0) FROM InstructorActivity a
        WHERE a.instructorId = :instructorId AND a.kind = :kind AND a.targetId = :targetId AND a.createdAt >= :since
    """)
    long sumItemsForTargetSince(@Param("instructorId") String instructorId,
                                @Param("kind") InboxKind kind,
                                @Param("targetId") String targetId,
                                @Param("since") LocalDateTime since);
}
