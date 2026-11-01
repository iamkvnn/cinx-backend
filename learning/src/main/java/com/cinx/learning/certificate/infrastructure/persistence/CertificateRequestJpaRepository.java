package com.cinx.learning.certificate.infrastructure.persistence;

import com.cinx.learning.shared.domain.CertificateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
interface CertificateRequestJpaRepository extends JpaRepository<CertificateRequestJpaEntity, String> {
    Optional<CertificateRequestJpaEntity> findByUserIdAndCourseId(String userId, String courseId);
    Page<CertificateRequestJpaEntity> findByCourseId(String courseId, Pageable pageable);
    Page<CertificateRequestJpaEntity> findByCourseIdAndStatus(String courseId, CertificateStatus status, Pageable pageable);
    Page<CertificateRequestJpaEntity> findByStatus(CertificateStatus status, Pageable pageable);
    List<CertificateRequestJpaEntity> findByUserIdAndStatus(String userId, CertificateStatus status);
    List<CertificateRequestJpaEntity> findByUserId(String userId);
    List<CertificateRequestJpaEntity> findAllByCourseIdAndStatus(String courseId, CertificateStatus status);
    List<CertificateRequestJpaEntity> findAllByCourseId(String courseId);

    /** Requests of these courses in one status: count and the oldest request, per course. */
    @Query("""
        SELECT r.courseId AS courseId, COUNT(r) AS waiting, MIN(r.requestedAt) AS oldest
        FROM CertificateRequest r
        WHERE r.courseId IN :courseIds AND r.status = :status
        GROUP BY r.courseId
    """)
    List<StatusGroup> findGroups(java.util.Collection<String> courseIds, CertificateStatus status);

    interface StatusGroup {
        String getCourseId();
        long getWaiting();
        java.time.LocalDateTime getOldest();
    }

    @Query("""
        SELECT c
        FROM CertificateRequest c
        WHERE (:courseId IS NULL OR c.courseId = :courseId)
          AND (:status IS NULL OR c.status = :status)
          AND (:query IS NULL OR c.id LIKE %:query% OR c.userId LIKE %:query% OR c.courseId LIKE %:query%)
    """)
    Page<CertificateRequestJpaEntity> search(String courseId, CertificateStatus status, String query, Pageable pageable);

    @Query("""
        SELECT c
        FROM CertificateRequest c
        WHERE c.courseId IN :courseIds
          AND (:status IS NULL OR c.status = :status)
          AND (:query IS NULL OR c.id LIKE %:query% OR c.userId LIKE %:query% OR c.courseId LIKE %:query%)
    """)
    Page<CertificateRequestJpaEntity> searchByCourseIds(List<String> courseIds, CertificateStatus status, String query, Pageable pageable);
}
