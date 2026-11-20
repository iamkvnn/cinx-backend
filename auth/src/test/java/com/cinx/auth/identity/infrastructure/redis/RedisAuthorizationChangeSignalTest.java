package com.cinx.auth.identity.infrastructure.redis;

import com.cinx.auth.identity.application.event.AuthorizationChanged;
import com.cinx.auth.identity.domain.IdentityStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAuthorizationChangeSignalTest {
    static final Instant CHANGED = Instant.parse("2026-11-14T10:00:00.700Z");

    @Mock StringRedisTemplate redis;
    @InjectMocks RedisAuthorizationChangeSignal signal;

    @Test
    void writesTheChangeUnderTheUsersKeyForLongerThanAnAccessToken() {
        signal.onCommitted(new AuthorizationChanged("user-1", IdentityStatus.BANNED, CHANGED));

        verify(redis).execute(RedisAuthorizationChangeSignal.SET_IF_NEWER, List.of("authz:changed:user-1"),
                "BANNED " + CHANGED.toEpochMilli(), String.valueOf(CHANGED.toEpochMilli()), "360");
    }

    @Test
    void anUnreachableRedisIsLoggedNotThrown() {
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> signal.onCommitted(new AuthorizationChanged("user-1", IdentityStatus.BANNED, CHANGED)))
                .doesNotThrowAnyException();
    }
}
