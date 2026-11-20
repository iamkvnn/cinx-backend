package com.cinx.learning.certificate.domain;

import com.cinx.learning.shared.domain.CertificateStatus;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where CertificateRequest rows are stored. */
public interface CertificateRequestRepository {
    Optional<CertificateRequest> findByUserIdAndCourseId(String userId, String courseId);

    List<CertificateRequest> findByUserIdAndStatus(String userId, CertificateStatus status);

    List<CertificateRequest> findByUserId(String userId);

    List<CertificateRequest> findAllByCourseIdAndStatus(String courseId, CertificateStatus status);

    List<CertificateRequest> findAllByCourseId(String courseId);

    /** Requests of these courses in one status: count and the oldest request, per course. */
    List<StatusGroup> findGroups(java.util.Collection<String> courseIds, CertificateStatus status);

    PageResult<CertificateRequest> search(String courseId, CertificateStatus status, String query, PageQuery pageable);

    PageResult<CertificateRequest> searchByCourseIds(List<String> courseIds, CertificateStatus status, String query, PageQuery pageable);

    Optional<CertificateRequest> findById(String id);

    CertificateRequest save(CertificateRequest entity);

    interface StatusGroup {
            String getCourseId();
            long getWaiting();
            java.time.LocalDateTime getOldest();
        }
}
