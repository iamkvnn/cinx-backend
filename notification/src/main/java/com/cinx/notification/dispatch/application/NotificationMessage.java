package com.cinx.notification.dispatch.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NotificationMessage(
        String type,
        String title,
        String message,
        String subject,
        String htmlBody,
        String referenceId,
        String actionUrl,
        Map<String, Object> metadata
) {
    public Map<String, Object> inAppPayload(List<String> userIds) {
        Map<String, Object> payload = basePayload();
        payload.put("userIds", userIds);
        return payload;
    }

    public Map<String, Object> pushPayload(List<String> userIds) {
        Map<String, Object> payload = basePayload();
        payload.put("userIds", userIds);
        payload.put("data", pushData());
        return payload;
    }

    public Map<String, Object> emailPayload(String to) {
        return Map.of(
                "to", to,
                "subject", subject,
                "body", htmlBody
        );
    }

    private Map<String, Object> basePayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("message", message);
        payload.put("type", type);
        if (referenceId != null) {
            payload.put("referenceId", referenceId);
        }
        if (actionUrl != null) {
            payload.put("actionUrl", actionUrl);
        }
        payload.put("metadata", metadata == null ? Map.of() : metadata);
        return payload;
    }

    private Map<String, String> pushData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", type);
        if (referenceId != null) {
            data.put("referenceId", referenceId);
        }
        if (actionUrl != null) {
            data.put("actionUrl", actionUrl);
        }
        if (metadata != null) {
            metadata.forEach((key, value) -> {
                if (value != null) {
                    data.put(key, String.valueOf(value));
                }
            });
        }
        return data;
    }
}
