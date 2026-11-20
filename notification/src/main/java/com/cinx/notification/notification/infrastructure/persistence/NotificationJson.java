package com.cinx.notification.notification.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class NotificationJson {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private NotificationJson() {
    }

    public static String write(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid notification metadata", e);
        }
    }

    public static Map<String, Object> read(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(metadataJson, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
