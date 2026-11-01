package com.cinx.learning.certificate.infrastructure.persistence;

import com.cinx.learning.certificate.domain.CourseCertificateConditions;
import com.cinx.learning.certificate.domain.CourseCertificateConditionsRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseCertificateConditionsRepositoryAdapter implements CourseCertificateConditionsRepository {
    private final CourseCertificateConditionsJpaRepository jpa;
    private final CourseCertificateConditionsPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseCertificateConditions> findByCourseId(String courseId) {
        return jpa.findByCourseId(courseId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(CourseCertificateConditions entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public CourseCertificateConditions saveAndFlush(CourseCertificateConditions entity) {
        CourseCertificateConditions saved = store(entity);
        jpa.flush();
        return saved;
    }

    /** Inserts a new CourseCertificateConditions, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseCertificateConditions store(CourseCertificateConditions entity) {
        CourseCertificateConditionsJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
