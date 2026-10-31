package com.cinx.learning.instructor.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.domain.InstructorActivity;
import com.cinx.learning.instructor.domain.InstructorActivityRepository;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class InstructorActivityRepositoryAdapter implements InstructorActivityRepository {
    private final InstructorActivityJpaRepository jpa;
    private final InstructorActivityPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorActivity> findAllByCourseId(String courseId) {
        return jpa.findAllByCourseId(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorActivity> findRecent(String instructorId, LocalDateTime since, InboxKind kind, String courseId, PageQuery pageable) {
        return jpa.findRecent(instructorId, since, kind, courseId, Pages.pageable(pageable)).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public long sumItemsSince(String instructorId, LocalDateTime since) {
        return jpa.sumItemsSince(instructorId, since);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<InstructorActivity> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional
    public InstructorActivity save(InstructorActivity entity) {
        return store(entity);
    }

    /** Inserts a new InstructorActivity, or writes the changes onto the stored row and leaves the lock version to the store. */
    private InstructorActivity store(InstructorActivity entity) {
        InstructorActivityJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
