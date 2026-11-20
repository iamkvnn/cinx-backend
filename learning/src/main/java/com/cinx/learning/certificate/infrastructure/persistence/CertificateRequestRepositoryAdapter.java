package com.cinx.learning.certificate.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.certificate.domain.CertificateRequest;
import com.cinx.learning.certificate.domain.CertificateRequestRepository;
import com.cinx.learning.shared.domain.CertificateStatus;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CertificateRequestRepositoryAdapter implements CertificateRequestRepository {
    private final CertificateRequestJpaRepository jpa;
    private final CertificateRequestPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CertificateRequest> findByUserIdAndCourseId(String userId, String courseId) {
        return jpa.findByUserIdAndCourseId(userId, courseId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateRequest> findByUserIdAndStatus(String userId, CertificateStatus status) {
        return jpa.findByUserIdAndStatus(userId, status).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateRequest> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateRequest> findAllByCourseIdAndStatus(String courseId, CertificateStatus status) {
        return jpa.findAllByCourseIdAndStatus(courseId, status).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateRequest> findAllByCourseId(String courseId) {
        return jpa.findAllByCourseId(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusGroup> findGroups(java.util.Collection<String> courseIds, CertificateStatus status) {
        return jpa.findGroups(courseIds, status).stream().map(CertificateRequestRepositoryAdapter::wrapStatusGroup).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CertificateRequest> search(String courseId, CertificateStatus status, String query, PageQuery pageable) {
        return Pages.result(jpa.search(courseId, status, query, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CertificateRequest> searchByCourseIds(List<String> courseIds, CertificateStatus status, String query, PageQuery pageable) {
        return Pages.result(jpa.searchByCourseIds(courseIds, status, query, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CertificateRequest> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CertificateRequest save(CertificateRequest entity) {
        return store(entity);
    }

    /** Inserts a new CertificateRequest, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CertificateRequest store(CertificateRequest entity) {
        CertificateRequestJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static CertificateRequestRepository.StatusGroup wrapStatusGroup(CertificateRequestJpaRepository.StatusGroup row) {
        if (row == null) {
            return null;
        }
        return new CertificateRequestRepository.StatusGroup() {
            @Override
            public String getCourseId() {
                return row.getCourseId();
            }
            @Override
            public long getWaiting() {
                return row.getWaiting();
            }
            @Override
            public java.time.LocalDateTime getOldest() {
                return row.getOldest();
            }
        };
    }
}
