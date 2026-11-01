package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.community.domain.CourseAnswer;
import com.cinx.learning.community.domain.CourseAnswerRepository;
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
class CourseAnswerRepositoryAdapter implements CourseAnswerRepository {
    private final CourseAnswerJpaRepository jpa;
    private final CourseAnswerPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseAnswer> findByQuestionIdAndParentAnswerIdIsNullOrderByCreatedAtAsc(String questionId, PageQuery pageable) {
        return Pages.result(jpa.findByQuestionIdAndParentAnswerIdIsNullOrderByCreatedAtAsc(questionId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseAnswer> findByParentAnswerIdOrderByCreatedAtAsc(String parentAnswerId, PageQuery pageable) {
        return Pages.result(jpa.findByParentAnswerIdOrderByCreatedAtAsc(parentAnswerId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseAnswer> findByQuestionId(String questionId) {
        return jpa.findByQuestionId(questionId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseAnswer> findByParentAnswerId(String parentAnswerId) {
        return jpa.findByParentAnswerId(parentAnswerId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteByIdIn(Collection<String> answerIds) {
        jpa.deleteByIdIn(answerIds);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByQuestionId(String questionId) {
        return jpa.countByQuestionId(questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByParentAnswerId(String parentAnswerId) {
        return jpa.countByParentAnswerId(parentAnswerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countAnswersByCourseIdBetween(String courseId, LocalDateTime start, LocalDateTime end) {
        return jpa.countAnswersByCourseIdBetween(courseId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseAnswer> findInstructorAnswersInLesson(String lessonId, String userId, PageQuery pageable) {
        return jpa.findInstructorAnswersInLesson(lessonId, userId, Pages.pageable(pageable)).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseAnswer> findAllById(Iterable<String> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseAnswer> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CourseAnswer save(CourseAnswer entity) {
        return store(entity);
    }

    /** Inserts a new CourseAnswer, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseAnswer store(CourseAnswer entity) {
        CourseAnswerJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
