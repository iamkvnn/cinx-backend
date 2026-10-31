package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.assessment.domain.AssignmentSubmission;
import com.cinx.learning.assessment.domain.AssignmentSubmissionRepository;
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
class AssignmentSubmissionRepositoryAdapter implements AssignmentSubmissionRepository {
    private final AssignmentSubmissionJpaRepository jpa;
    private final AssignmentSubmissionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssignmentSubmission> findAllByAssignmentId(String assignmentId, PageQuery pageable) {
        return Pages.result(jpa.findAllByAssignmentId(assignmentId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmission> findAllByAssignmentId(String assignmentId) {
        return jpa.findAllByAssignmentId(assignmentId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UngradedGroup> findUngradedGroups(Collection<String> assignmentIds) {
        return jpa.findUngradedGroups(assignmentIds).stream().map(AssignmentSubmissionRepositoryAdapter::wrapUngradedGroup).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public long countGradedSince(String assignmentId, java.time.LocalDateTime since) {
        return jpa.countGradedSince(assignmentId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentSubmission> findByUserIdAndAssignmentId(String userId, String assignmentId) {
        return jpa.findByUserIdAndAssignmentId(userId, assignmentId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findSubmittedAssignmentIds(String userId, Collection<String> assignmentIds) {
        return jpa.findSubmittedAssignmentIds(userId, assignmentIds);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentSubmission> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public AssignmentSubmission save(AssignmentSubmission entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public AssignmentSubmission saveAndFlush(AssignmentSubmission entity) {
        AssignmentSubmission saved = store(entity);
        jpa.flush();
        return saved;
    }

    /** Inserts a new AssignmentSubmission, or writes the changes onto the stored row and leaves the lock version to the store. */
    private AssignmentSubmission store(AssignmentSubmission entity) {
        AssignmentSubmissionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static AssignmentSubmissionRepository.UngradedGroup wrapUngradedGroup(AssignmentSubmissionJpaRepository.UngradedGroup row) {
        if (row == null) {
            return null;
        }
        return new AssignmentSubmissionRepository.UngradedGroup() {
            @Override
            public String getAssignmentId() {
                return row.getAssignmentId();
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
