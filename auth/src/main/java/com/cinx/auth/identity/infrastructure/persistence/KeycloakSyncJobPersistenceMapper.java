package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.KeycloakSyncJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface KeycloakSyncJobPersistenceMapper {
    KeycloakSyncJobJpaEntity toEntity(KeycloakSyncJob job);

    KeycloakSyncJob toDomain(KeycloakSyncJobJpaEntity entity);

    /** Copies what a job's life changes onto the stored row; its key, owner and version stay as they were. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "desiredAuthorizationVersion", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(@MappingTarget KeycloakSyncJobJpaEntity entity, KeycloakSyncJob job);
}
