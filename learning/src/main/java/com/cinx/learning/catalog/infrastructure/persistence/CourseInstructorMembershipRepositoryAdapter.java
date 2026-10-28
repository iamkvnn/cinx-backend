package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseInstructorMembership;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseInstructorMembershipRepositoryAdapter implements CourseInstructorMembershipRepository {
    private final CourseInstructorMembershipJpaRepository jpa;
    private final CourseInstructorMembershipPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseInstructorMembership> findByCourseIdAndInstructorId(String courseId, String instructorId) {
        return jpa.findByCourseIdAndInstructorId(courseId, instructorId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseInstructorMembership> findByCourseIdOrderByCreatedAtAsc(String courseId) {
        return jpa.findByCourseIdOrderByCreatedAtAsc(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseInstructorMembership> findByCourseIdAndStatusOrderByCreatedAtAsc(String courseId, CourseMembershipStatus status) {
        return jpa.findByCourseIdAndStatusOrderByCreatedAtAsc(courseId, status).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseInstructorMembership> findByInstructorIdAndStatus(String instructorId, CourseMembershipStatus status) {
        return jpa.findByInstructorIdAndStatus(instructorId, status).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<CourseInstructorMembership> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional
    public CourseInstructorMembership save(CourseInstructorMembership entity) {
        return store(entity);
    }

    /** Inserts a new CourseInstructorMembership, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseInstructorMembership store(CourseInstructorMembership entity) {
        CourseInstructorMembershipJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
