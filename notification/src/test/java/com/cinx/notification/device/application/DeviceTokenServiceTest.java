package com.cinx.notification.device.application;

import com.cinx.notification.device.application.command.RegisterDeviceTokenCommand;
import com.cinx.notification.device.domain.UserDeviceToken;
import com.cinx.notification.device.domain.UserDeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {
    @Mock
    private UserDeviceTokenRepository repository;
    private DeviceTokenService service;

    @BeforeEach
    void setUp() {
        service = new DeviceTokenService(repository);
    }

    @Test
    void anUnknownTokenIsRegisteredForTheUser() {
        when(repository.findByFcmToken("fcm-1")).thenReturn(Optional.empty());

        service.register("user-1", new RegisterDeviceTokenCommand("fcm-1", "Pixel 8"));

        ArgumentCaptor<UserDeviceToken> saved = ArgumentCaptor.forClass(UserDeviceToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getFcmToken()).isEqualTo("fcm-1");
        assertThat(saved.getValue().getUserId()).isEqualTo("user-1");
        assertThat(saved.getValue().getDeviceInfo()).isEqualTo("Pixel 8");
    }

    @Test
    void aKnownTokenMovesToTheUserWhoRegistersItNext() {
        // a shared device: the token row is reused, not duplicated, and now belongs to the new user
        UserDeviceToken existing = UserDeviceToken.builder().id("token-row-1").userId("user-1").fcmToken("fcm-1").deviceInfo("old").build();
        when(repository.findByFcmToken("fcm-1")).thenReturn(Optional.of(existing));

        service.register("user-2", new RegisterDeviceTokenCommand("fcm-1", "iPad"));

        verify(repository).save(existing);
        assertThat(existing.getId()).isEqualTo("token-row-1");
        assertThat(existing.getUserId()).isEqualTo("user-2");
        assertThat(existing.getDeviceInfo()).isEqualTo("iPad");
    }

    @Test
    void findTokensReturnsOnlyTheTokenStrings() {
        when(repository.findByUserId("user-1")).thenReturn(List.of(
                UserDeviceToken.builder().id("1").userId("user-1").fcmToken("fcm-1").build(),
                UserDeviceToken.builder().id("2").userId("user-1").fcmToken("fcm-2").build()));

        assertThat(service.findTokens("user-1")).containsExactly("fcm-1", "fcm-2");
    }
}
