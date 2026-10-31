package com.cinx.learning.enrollment.infrastructure.persistence;

import com.cinx.learning.enrollment.domain.CourseEntitlement;
import com.cinx.learning.enrollment.domain.CourseEntitlementRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseEntitlementRepositoryAdapter implements CourseEntitlementRepository {
    private final CourseEntitlementJpaRepository jpa;
    private final CourseEntitlementPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCourseId(String userId, String courseId) {
        return jpa.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCourseId(String courseId) {
        return jpa.existsByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEntitlement> findAllByCourseId(String courseId) {
        return jpa.findAllByCourseId(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEntitlement> findAllByUserIdAndCourseIdIn(String userId, Collection<String> courseIds) {
        return jpa.findAllByUserIdAndCourseIdIn(userId, courseIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEntitlement> findAllByUserIdOrderByGrantedAtDesc(String userId) {
        return jpa.findAllByUserIdOrderByGrantedAtDesc(userId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUserIdsByCourseId(String courseId) {
        return jpa.findUserIdsByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCount> countPerCourse(Collection<String> courseIds) {
        return jpa.countPerCourse(courseIds).stream().map(CourseEntitlementRepositoryAdapter::wrapCourseCount).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrantRow> findGrantsBetween(Collection<String> courseIds, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return jpa.findGrantsBetween(courseIds, start, end).stream().map(CourseEntitlementRepositoryAdapter::wrapGrantRow).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return jpa.existsById(id);
    }

    @Override
    @Transactional
    public CourseEntitlement saveAndFlush(CourseEntitlement entity) {
        CourseEntitlement saved = store(entity);
        jpa.flush();
        return saved;
    }

    /** Inserts a new CourseEntitlement, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseEntitlement store(CourseEntitlement entity) {
        CourseEntitlementJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static CourseEntitlementRepository.CourseCount wrapCourseCount(CourseEntitlementJpaRepository.CourseCount row) {
        if (row == null) {
            return null;
        }
        return new CourseEntitlementRepository.CourseCount() {
            @Override
            public String getCourseId() {
                return row.getCourseId();
            }
            @Override
            public long getTotal() {
                return row.getTotal();
            }
        };
    }

    private static CourseEntitlementRepository.GrantRow wrapGrantRow(CourseEntitlementJpaRepository.GrantRow row) {
        if (row == null) {
            return null;
        }
        return new CourseEntitlementRepository.GrantRow() {
            @Override
            public String getCourseId() {
                return row.getCourseId();
            }
            @Override
            public java.time.LocalDateTime getGrantedAt() {
                return row.getGrantedAt();
            }
        };
    }
}
