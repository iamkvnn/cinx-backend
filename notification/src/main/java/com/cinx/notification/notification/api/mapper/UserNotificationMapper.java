package com.cinx.notification.notification.api.mapper;

import com.cinx.notification.notification.api.dto.request.CreateNotificationRequest;
import com.cinx.notification.notification.api.dto.response.NotificationCountsResponse;
import com.cinx.notification.notification.api.dto.response.UserNotificationResponse;
import com.cinx.notification.notification.application.command.SendNotificationCommand;
import com.cinx.notification.notification.application.query.NotificationCounts;
import com.cinx.notification.notification.application.query.UserNotificationView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserNotificationMapper {
    @Mapping(target = "isRead", source = "read")
    UserNotificationResponse toDto(UserNotificationView view);

    NotificationCountsResponse toDto(NotificationCounts counts);

    SendNotificationCommand toCommand(CreateNotificationRequest request);
}
