package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.application.query.UserNotificationView;
import com.cinx.notification.notification.domain.UserNotification;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotificationPersistenceMapperTest {
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2025, 1, 1, 10, 0);
    private final UserNotificationPersistenceMapper mapper = Mappers.getMapper(UserNotificationPersistenceMapper.class);

    @Test
    void domainToEntityAndBackKeepsEveryField() {
        UserNotification original = UserNotification.builder().id("row-1").userId("user-1").notificationId("notification-1")
                .read(true).sentAt(SENT_AT).build();

        UserNotification roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void theReadFlagIsWrittenToTheIsReadColumnAndTheJoinIsLeftEmpty() {
        UserNotificationJpaEntity entity = mapper.toEntity(UserNotification.unread("user-1", "notification-1", SENT_AT));

        assertThat(entity.getIsRead()).isFalse();
        assertThat(entity.getNotificationId()).isEqualTo("notification-1");
        // the content is joined on read only; writing it from here would be a second source of truth
        assertThat(entity.getNotification()).isNull();
    }

    @Test
    void aRowWhoseFlagWasNeverWrittenCountsAsUnread() {
        UserNotificationJpaEntity entity = UserNotificationJpaEntity.builder().id("row-1").userId("user-1")
                .notificationId("notification-1").isRead(null).sentAt(SENT_AT).build();

        assertThat(mapper.toDomain(entity).isRead()).isFalse();
    }

    @Test
    void theViewCarriesTheJoinedContentTheFlagAndTheParsedMetadata() {
        NotificationJpaEntity content = NotificationJpaEntity.builder().id("notification-1").title("Title").message("Message")
                .type("COURSE_PUBLISHED").referenceId("course-1").actionUrl("/courses/course-1")
                .metadataJson("{\"courseId\":\"course-1\"}").build();
        UserNotificationJpaEntity entity = UserNotificationJpaEntity.builder().id("row-1").userId("user-1")
                .notificationId("notification-1").isRead(true).sentAt(SENT_AT).notification(content).build();

        UserNotificationView view = mapper.toView(entity);

        assertThat(view).isEqualTo(new UserNotificationView("row-1", "user-1", "Title", "Message", "COURSE_PUBLISHED",
                "course-1", "/courses/course-1", Map.of("courseId", "course-1"), true, SENT_AT));
    }
}
