package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPersistenceMapperTest {
    private final NotificationPersistenceMapper mapper = Mappers.getMapper(NotificationPersistenceMapper.class);

    @Test
    void domainToEntityAndBackKeepsEveryField() {
        Notification original = Notification.builder().id("notification-1").title("Title").message("Message")
                .type("COURSE_PUBLISHED").referenceId("course-1").actionUrl("/courses/course-1")
                .metadata(Map.of("courseId", "course-1", "count", 3)).build();

        Notification roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void metadataIsStoredAsJsonText() {
        NotificationJpaEntity entity = mapper.toEntity(Notification.builder().title("T").message("M")
                .metadata(Map.of("courseId", "course-1")).build());

        assertThat(entity.getMetadataJson()).isEqualTo("{\"courseId\":\"course-1\"}");
        assertThat(entity.getId()).isNull();
    }

    @Test
    void noMetadataIsStoredAsNullAndReadsBackAsAnEmptyMap() {
        NotificationJpaEntity entity = mapper.toEntity(Notification.builder().title("T").message("M").build());

        assertThat(entity.getMetadataJson()).isNull();
        assertThat(mapper.toDomain(entity).getMetadata()).isEmpty();
    }

    @Test
    void anUnreadableMetadataColumnReadsBackAsAnEmptyMapInsteadOfFailing() {
        NotificationJpaEntity entity = NotificationJpaEntity.builder().id("n-1").metadataJson("{bad json").build();

        assertThat(mapper.toDomain(entity).getMetadata()).isEmpty();
    }
}
