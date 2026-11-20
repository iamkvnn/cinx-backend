package com.cinx.notification.device.infrastructure.persistence;

import com.cinx.notification.device.domain.UserDeviceToken;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserDeviceTokenPersistenceMapperTest {
    private final UserDeviceTokenPersistenceMapper mapper = Mappers.getMapper(UserDeviceTokenPersistenceMapper.class);

    @Test
    void domainToEntityAndBackKeepsEveryDomainField() {
        UserDeviceToken original = UserDeviceToken.builder().id("token-1").userId("user-1").fcmToken("fcm-1").deviceInfo("Pixel 8").build();

        UserDeviceToken roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void theUpdateTimeIsNeverTakenFromTheDomain() {
        UserDeviceTokenJpaEntity entity = mapper.toEntity(UserDeviceToken.forToken("fcm-1"));

        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void theUpdateTimeIsNotPartOfTheDomain() {
        UserDeviceTokenJpaEntity entity = UserDeviceTokenJpaEntity.builder().id("token-1").userId("user-1").fcmToken("fcm-1")
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0)).build();

        UserDeviceToken token = mapper.toDomain(entity);

        assertThat(token.getFcmToken()).isEqualTo("fcm-1");
        assertThat(token.getDeviceInfo()).isNull();
    }
}
