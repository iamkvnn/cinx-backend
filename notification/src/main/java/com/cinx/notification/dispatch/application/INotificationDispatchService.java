package com.cinx.notification.dispatch.application;

import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;

public interface INotificationDispatchService {
    void dispatch(NotificationContext context);
}
