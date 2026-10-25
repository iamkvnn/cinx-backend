package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.KeycloakSyncJob;
import com.cinx.auth.identity.domain.KeycloakSyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class KeycloakSyncJobRepositoryAdapter implements KeycloakSyncJobRepository {
    private final KeycloakSyncJobJpaRepository jobs;
    private final KeycloakSyncJobPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndDesiredAuthorizationVersion(String userId, long version) {
        return jobs.existsByUserIdAndDesiredAuthorizationVersion(userId, version);
    }

    @Override
    @Transactional
    public KeycloakSyncJob save(KeycloakSyncJob job) {
        KeycloakSyncJobJpaEntity entity = jobs.findById(job.getId()).orElse(null);
        if (entity == null) {
            return mapper.toDomain(jobs.save(mapper.toEntity(job)));
        }
        mapper.update(entity, job);
        return mapper.toDomain(jobs.save(entity));
    }

    @Override
    @Transactional
    public List<KeycloakSyncJob> lockDue(LocalDateTime now) {
        return jobs.lockDue(now).stream().map(mapper::toDomain).toList();
    }
}
