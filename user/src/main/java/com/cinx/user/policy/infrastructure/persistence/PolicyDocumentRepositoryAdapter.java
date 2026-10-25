package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.user.policy.domain.PolicyDocument;
import com.cinx.user.policy.domain.PolicyDocumentRepository;
import com.cinx.user.policy.domain.PolicyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PolicyDocumentRepositoryAdapter implements PolicyDocumentRepository {
    private final PolicyDocumentJpaRepository jpa;
    private final PolicyDocumentPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyDocument> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    /** Inserts a new document, or writes the changes onto the stored row so its lock version and untouched sections stay put. */
    @Override
    @Transactional
    public PolicyDocument save(PolicyDocument document) {
        if (document.getId() == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(document)));
        }
        PolicyDocumentJpaEntity entity = jpa.findById(document.getId())
                .orElseThrow(() -> new IllegalStateException("Policy " + document.getId() + " no longer exists"));
        mapper.update(entity, document);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public int findMaxVersionNumberBySlug(String slug) {
        Integer max = jpa.findMaxVersionNumberBySlug(slug);
        return max == null ? 0 : max;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyDocument> findLatestPublishedBySlug(String slug) {
        return jpa.findFirstBySlugAndStatusOrderByVersionNumberDesc(slug, PolicyStatus.PUBLISHED).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyDocument> findAllPublishedBySlug(String slug) {
        return jpa.findAllBySlugAndStatus(slug, PolicyStatus.PUBLISHED).stream().map(mapper::toDomain).toList();
    }
}
