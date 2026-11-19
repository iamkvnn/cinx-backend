package com.cinx.notification.device.infrastructure.persistence;

import com.cinx.notification.device.domain.UserDeviceToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface UserDeviceTokenPersistenceMapper {
    // the update time is the entity's own: it is stamped on every insert and update
    @Mapping(target = "updatedAt", ignore = true)
    UserDeviceTokenJpaEntity toEntity(UserDeviceToken token);

    UserDeviceToken toDomain(UserDeviceTokenJpaEntity entity);
}
