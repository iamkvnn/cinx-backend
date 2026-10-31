package com.cinx.learning.enrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

interface CourseEntitlementJpaRepository extends JpaRepository<CourseEntitlementJpaEntity, String> {
    boolean existsByUserIdAndCourseId(String userId, String courseId);
    boolean existsByCourseId(String courseId);
    List<CourseEntitlementJpaEntity> findAllByCourseId(String courseId);
    List<CourseEntitlementJpaEntity> findAllByUserIdAndCourseIdIn(String userId, Collection<String> courseIds);
    List<CourseEntitlementJpaEntity> findAllByUserIdOrderByGrantedAtDesc(String userId);

    @Query("select e.userId from CourseEntitlement e where e.courseId = :courseId")
    List<String> findUserIdsByCourseId(String courseId);

    @Query("select e.courseId as courseId, count(e) as total from CourseEntitlement e where e.courseId in :courseIds group by e.courseId")
    List<CourseCount> countPerCourse(Collection<String> courseIds);

    /** When each learner of these courses was granted access, within a window (bucketed by day by the caller). */
    @Query("""
        select e.courseId as courseId, e.grantedAt as grantedAt
        from CourseEntitlement e
        where e.courseId in :courseIds and e.grantedAt >= :start and e.grantedAt < :end
    """)
    List<GrantRow> findGrantsBetween(Collection<String> courseIds, java.time.LocalDateTime start, java.time.LocalDateTime end);

    interface CourseCount {
        String getCourseId();
        long getTotal();
    }

    interface GrantRow {
        String getCourseId();
        java.time.LocalDateTime getGrantedAt();
    }
}
