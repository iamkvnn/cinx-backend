package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.domain.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = NotificationJson.class)
interface NotificationPersistenceMapper {
    @Mapping(target = "metadataJson", expression = "java(NotificationJson.write(notification.getMetadata()))")
    NotificationJpaEntity toEntity(Notification notification);

    @Mapping(target = "metadata", expression = "java(NotificationJson.read(entity.getMetadataJson()))")
    Notification toDomain(NotificationJpaEntity entity);
}
