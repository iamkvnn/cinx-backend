package com.cinx.commerce.subscription.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthEligibilityAdapterTest {
    private final AuthEligibilityProjectionRepository projections = mock(AuthEligibilityProjectionRepository.class);
    private final AuthEligibilityAdapter adapter = new AuthEligibilityAdapter(projections);

    @Test
    void anAccountAuthNeverAnnouncedIsActive() {
        when(projections.findById("user-1")).thenReturn(Optional.empty());

        assertThat(adapter.isActive("user-1")).isTrue();
    }

    @Test
    void aBannedAccountIsNotActive() {
        when(projections.findById("user-1")).thenReturn(Optional.of(
                AuthEligibilityProjection.builder().userId("user-1").accountStatus("BANNED").build()));

        assertThat(adapter.isActive("user-1")).isFalse();
    }
}
