package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.engagement.domain.UserStreak;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface UserStreakPersistenceMapper {
    UserStreak toDomain(UserStreakJpaEntity entity);

    UserStreakJpaEntity toEntity(UserStreak domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget UserStreakJpaEntity entity, UserStreak domain);
}
