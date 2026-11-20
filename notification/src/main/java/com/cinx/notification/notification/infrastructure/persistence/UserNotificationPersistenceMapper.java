package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.application.query.UserNotificationView;
import com.cinx.notification.notification.domain.UserNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {NotificationJson.class, Boolean.class})
interface UserNotificationPersistenceMapper {
    @Mapping(target = "isRead", source = "read")
    @Mapping(target = "notification", ignore = true)
    UserNotificationJpaEntity toEntity(UserNotification userNotification);

    // a row whose flag was never written counts as unread
    @Mapping(target = "read", expression = "java(Boolean.TRUE.equals(entity.getIsRead()))")
    UserNotification toDomain(UserNotificationJpaEntity entity);

    @Mapping(target = "title", source = "notification.title")
    @Mapping(target = "message", source = "notification.message")
    @Mapping(target = "type", source = "notification.type")
    @Mapping(target = "referenceId", source = "notification.referenceId")
    @Mapping(target = "actionUrl", source = "notification.actionUrl")
    @Mapping(target = "metadata", expression = "java(NotificationJson.read(entity.getNotification().getMetadataJson()))")
    @Mapping(target = "read", expression = "java(Boolean.TRUE.equals(entity.getIsRead()))")
    UserNotificationView toView(UserNotificationJpaEntity entity);
}
