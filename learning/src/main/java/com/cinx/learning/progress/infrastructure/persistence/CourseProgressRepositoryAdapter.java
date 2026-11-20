package com.cinx.learning.progress.infrastructure.persistence;

import com.cinx.learning.progress.domain.CourseProgress;
import com.cinx.learning.progress.domain.CourseProgressRepository;
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
class CourseProgressRepositoryAdapter implements CourseProgressRepository {
    private final CourseProgressJpaRepository jpa;
    private final CourseProgressPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgress> findAllByUserId(String userId) {
        return jpa.findAllByUserId(userId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgress> findAllByUserIdAndCourseIdIn(String userId, List<String> courseIds) {
        return jpa.findAllByUserIdAndCourseIdIn(userId, courseIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseProgress> findByUserIdAndCourseId(String userId, String courseId) {
        return jpa.findByUserIdAndCourseId(userId, courseId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCourseId(String userId, String courseId) {
        return jpa.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgress> findAllByCourseId(String courseId) {
        return jpa.findAllByCourseId(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgress> findAllByCourseIdIn(List<String> courseIds) {
        return jpa.findAllByCourseIdIn(courseIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompletionRow> completionPerCourse(java.util.Collection<String> courseIds) {
        return jpa.completionPerCourse(courseIds).stream().map(CourseProgressRepositoryAdapter::wrapCompletionRow).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public CourseProgress save(CourseProgress entity) {
        return store(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseProgress> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    /** Inserts a new CourseProgress, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseProgress store(CourseProgress entity) {
        CourseProgressJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static CourseProgressRepository.CompletionRow wrapCompletionRow(CourseProgressJpaRepository.CompletionRow row) {
        if (row == null) {
            return null;
        }
        return new CourseProgressRepository.CompletionRow() {
            @Override
            public String getCourseId() {
                return row.getCourseId();
            }
            @Override
            public long getTotal() {
                return row.getTotal();
            }
            @Override
            public Long getCompleted() {
                return row.getCompleted();
            }
        };
    }
}
