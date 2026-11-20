package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.community.domain.CourseQuestion;
import com.cinx.learning.community.domain.CourseQuestionRepository;
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
class CourseQuestionRepositoryAdapter implements CourseQuestionRepository {
    private final CourseQuestionJpaRepository jpa;
    private final CourseQuestionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<LessonCount> countPerLesson(String courseId) {
        return jpa.countPerLesson(courseId).stream().map(CourseQuestionRepositoryAdapter::wrapLessonCount).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseQuestion> search(String courseId, String lessonId, String query, boolean unanswered, PageQuery pageable) {
        return Pages.result(jpa.search(courseId, lessonId, query, unanswered, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseQuestion> findSimilar(String courseId, String lessonId, String terms, double minScore, int limit) {
        return jpa.findSimilar(courseId, lessonId, terms, minScore, limit).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseQuestion> findUnansweredByLessonId(String lessonId) {
        return jpa.findUnansweredByLessonId(lessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnansweredGroup> findUnansweredGroups(java.util.Collection<String> courseIds) {
        return jpa.findUnansweredGroups(courseIds).stream().map(CourseQuestionRepositoryAdapter::wrapUnansweredGroup).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Long countQuestionsByCourseIdBetween(String courseId, LocalDateTime start, LocalDateTime end) {
        return jpa.countQuestionsByCourseIdBetween(courseId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countQuestionsWithInstructorAnswerBetween(String courseId, LocalDateTime start, LocalDateTime end) {
        return jpa.countQuestionsWithInstructorAnswerBetween(courseId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateQuestionsByDay(String courseId, LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateQuestionsByDay(courseId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateQuestionsByMonth(String courseId, LocalDateTime start, LocalDateTime end) {
        return jpa.aggregateQuestionsByMonth(courseId, start, end);
    }

    @Override
    @Transactional
    public void delete(CourseQuestion entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseQuestion> findAllById(Iterable<String> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseQuestion> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CourseQuestion save(CourseQuestion entity) {
        return store(entity);
    }

    /** Inserts a new CourseQuestion, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseQuestion store(CourseQuestion entity) {
        CourseQuestionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static CourseQuestionRepository.LessonCount wrapLessonCount(CourseQuestionJpaRepository.LessonCount row) {
        if (row == null) {
            return null;
        }
        return new CourseQuestionRepository.LessonCount() {
            @Override
            public String getLessonId() {
                return row.getLessonId();
            }
            @Override
            public long getTotal() {
                return row.getTotal();
            }
        };
    }

    private static CourseQuestionRepository.UnansweredGroup wrapUnansweredGroup(CourseQuestionJpaRepository.UnansweredGroup row) {
        if (row == null) {
            return null;
        }
        return new CourseQuestionRepository.UnansweredGroup() {
            @Override
            public String getCourseId() {
                return row.getCourseId();
            }
            @Override
            public String getLessonId() {
                return row.getLessonId();
            }
            @Override
            public long getWaiting() {
                return row.getWaiting();
            }
            @Override
            public long getUpvotes() {
                return row.getUpvotes();
            }
            @Override
            public LocalDateTime getOldest() {
                return row.getOldest();
            }
        };
    }
}
