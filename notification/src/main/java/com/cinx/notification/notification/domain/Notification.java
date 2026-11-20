package com.cinx.notification.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** The content of a notification: written once and shared by every recipient. The id is assigned when it is stored. */
@Getter
@Builder
@AllArgsConstructor
public class Notification {
    private final String id;
    private final String title;
    private final String message;
    private final String type;
    private final String referenceId;
    private final String actionUrl;
    private final Map<String, Object> metadata;
}
