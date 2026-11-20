package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
import java.time.LocalDateTime;
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
class QuizSessionRepositoryAdapter implements QuizSessionRepository {
    private final QuizSessionJpaRepository jpa;
    private final QuizSessionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<QuizSession> findAllByCourseIdAndStatusIn(String courseId, java.util.Collection<QuizSessionStatus> statuses) {
        return jpa.findAllByCourseIdAndStatusIn(courseId, statuses).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingGradeGroup> findPendingGradeGroups(java.util.Collection<String> courseIds) {
        return jpa.findPendingGradeGroups(courseIds).stream().map(QuizSessionRepositoryAdapter::wrapPendingGradeGroup).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findLessonIdsWithStatus(String userId, java.util.Collection<String> lessonIds, java.util.Collection<QuizSessionStatus> statuses) {
        return jpa.findLessonIdsWithStatus(userId, lessonIds, statuses);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuizSession> findAllByQuizLessonId(String quizLessonId, String userId, PageQuery pageable) {
        return Pages.result(jpa.findAllByQuizLessonId(quizLessonId, userId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer countByQuizLessonIdAndUserId(String quizLessonId, String userId) {
        return jpa.countByQuizLessonIdAndUserId(quizLessonId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByQuizLessonIdAndUserIdAndStatus(String quizLessonId, String userId, QuizSessionStatus status) {
        return jpa.existsByQuizLessonIdAndUserIdAndStatus(quizLessonId, userId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSession> findExpiredSessions(QuizSessionStatus status, LocalDateTime now) {
        return jpa.findExpiredSessions(status, now).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizSession> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public QuizSession save(QuizSession entity) {
        return store(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSession> findAllById(Iterable<String> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    /** Inserts a new QuizSession, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuizSession store(QuizSession entity) {
        QuizSessionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static QuizSessionRepository.PendingGradeGroup wrapPendingGradeGroup(QuizSessionJpaRepository.PendingGradeGroup row) {
        if (row == null) {
            return null;
        }
        return new QuizSessionRepository.PendingGradeGroup() {
            @Override
            public String getCourseId() {
                return row.getCourseId();
            }
            @Override
            public String getQuizLessonId() {
                return row.getQuizLessonId();
            }
            @Override
            public long getWaiting() {
                return row.getWaiting();
            }
            @Override
            public LocalDateTime getOldest() {
                return row.getOldest();
            }
        };
    }
}
