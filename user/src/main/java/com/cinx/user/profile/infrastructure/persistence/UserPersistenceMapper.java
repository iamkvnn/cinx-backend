package com.cinx.user.profile.infrastructure.persistence;

import com.cinx.user.profile.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface UserPersistenceMapper {
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserJpaEntity toEntity(User user);

    User toDomain(UserJpaEntity entity);

    /**
     * Copies what the domain may change onto the stored row. The key, the person's id, the lock version and the
     * audit columns stay the row's own: the store stamps them.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void update(@MappingTarget UserJpaEntity entity, User user);
}
