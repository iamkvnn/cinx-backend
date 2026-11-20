package com.cinx.notification.notification.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationJsonTest {

    @Test
    void writeAndReadMetadata() {
        String json = NotificationJson.write(Map.of("courseId", "course-1", "count", 2));

        assertThat(json).contains("courseId");
        assertThat(NotificationJson.read(json))
                .containsEntry("courseId", "course-1")
                .containsEntry("count", 2);
    }

    @Test
    void readInvalidMetadataReturnsEmptyMap() {
        assertThat(NotificationJson.read("{bad json")).isEmpty();
    }
}
