package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.engagement.domain.UserExperience;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface UserExperiencePersistenceMapper {
    UserExperience toDomain(UserExperienceJpaEntity entity);

    UserExperienceJpaEntity toEntity(UserExperience domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget UserExperienceJpaEntity entity, UserExperience domain);
}
