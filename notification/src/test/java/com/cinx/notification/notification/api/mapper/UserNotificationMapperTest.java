package com.cinx.notification.notification.api.mapper;

import com.cinx.notification.notification.application.query.NotificationCounts;
import com.cinx.notification.notification.application.query.UserNotificationView;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotificationMapperTest {
    private final UserNotificationMapper mapper = Mappers.getMapper(UserNotificationMapper.class);

    @Test
    void toDtoIncludesSentAtAndTheReadFlag() {
        LocalDateTime sentAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        UserNotificationView view = new UserNotificationView("user-notification-1", "user-1", "Title", "Message",
                "COURSE_QUESTION_CREATED", "question-1", "/courses/course-1/questions/question-1", Map.of("courseId", "course-1"), true, sentAt);

        var response = mapper.toDto(view);

        assertThat(response.sentAt()).isEqualTo(sentAt);
        assertThat(response.isRead()).isTrue();
        assertThat(response.id()).isEqualTo("user-notification-1");
        assertThat(response.metadata()).containsEntry("courseId", "course-1");
    }

    @Test
    void toDtoOfCountsKeepsTheNumbersAndThePerTypeMap() {
        var response = mapper.toDto(new NotificationCounts(8, 4, Map.of("COURSE_PUBLISHED", 5L)));

        assertThat(response.total()).isEqualTo(8);
        assertThat(response.unread()).isEqualTo(4);
        assertThat(response.byType()).containsEntry("COURSE_PUBLISHED", 5L);
    }
}
