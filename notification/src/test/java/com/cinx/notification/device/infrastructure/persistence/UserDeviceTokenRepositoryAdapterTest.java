package com.cinx.notification.device.infrastructure.persistence;

import com.cinx.notification.device.domain.UserDeviceToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeviceTokenRepositoryAdapterTest {
    @Mock
    private UserDeviceTokenJpaRepository jpa;
    private UserDeviceTokenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserDeviceTokenRepositoryAdapter(jpa, Mappers.getMapper(UserDeviceTokenPersistenceMapper.class));
    }

    @Test
    void aNewTokenIsInsertedAndComesBackWithItsId() {
        when(jpa.save(any(UserDeviceTokenJpaEntity.class))).thenAnswer(call -> {
            UserDeviceTokenJpaEntity entity = call.getArgument(0);
            entity.setId("token-1");
            return entity;
        });

        UserDeviceToken saved = adapter.save(UserDeviceToken.builder().userId("user-1").fcmToken("fcm-1").deviceInfo("Pixel 8").build());

        assertThat(saved.getId()).isEqualTo("token-1");
        assertThat(saved.getFcmToken()).isEqualTo("fcm-1");
    }

    @Test
    void anExistingTokenOnlyChangesHandsAndDeviceInfoNeverItsTokenString() {
        UserDeviceTokenJpaEntity stored = UserDeviceTokenJpaEntity.builder().id("token-1").userId("user-1").fcmToken("fcm-1").deviceInfo("old").build();
        when(jpa.findById("token-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);

        UserDeviceToken saved = adapter.save(UserDeviceToken.builder().id("token-1").userId("user-2").fcmToken("tampered").deviceInfo("iPad").build());

        assertThat(stored.getUserId()).isEqualTo("user-2");
        assertThat(stored.getDeviceInfo()).isEqualTo("iPad");
        assertThat(stored.getFcmToken()).isEqualTo("fcm-1");
        assertThat(saved.getUserId()).isEqualTo("user-2");
    }

    @Test
    void savingATokenThatVanishedFailsLoudly() {
        when(jpa.findById("token-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(UserDeviceToken.builder().id("token-1").userId("user-1").fcmToken("fcm-1").build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findersMapEntitiesToDomainObjects() {
        UserDeviceTokenJpaEntity stored = UserDeviceTokenJpaEntity.builder().id("token-1").userId("user-1").fcmToken("fcm-1").build();
        when(jpa.findByFcmToken("fcm-1")).thenReturn(Optional.of(stored));
        when(jpa.findByUserId("user-1")).thenReturn(List.of(stored));

        assertThat(adapter.findByFcmToken("fcm-1")).get().extracting(UserDeviceToken::getUserId).isEqualTo("user-1");
        assertThat(adapter.findByFcmToken("missing")).isEmpty();
        assertThat(adapter.findByUserId("user-1")).extracting(UserDeviceToken::getFcmToken).containsExactly("fcm-1");
    }
}
