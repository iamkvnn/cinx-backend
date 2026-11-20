package com.cinx.learning.entitlement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionFeatureAccessAdapterTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);
    private static final String FEATURE = "learning.subscription-catalog.access";

    @Mock
    private UserEntitlementProjectionRepository projectionRepository;
    @Mock
    private UserFeatureEntitlementRepository featureRepository;

    @InjectMocks
    private SubscriptionFeatureAccessAdapter adapter;

    @Test
    void anActiveSubscriptionWithTheFeatureEnabledGrantsIt() {
        projection("ACTIVE", null, NOW.plusDays(1));
        feature(true, null);

        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isTrue();
    }

    @Test
    void aSubscriptionInGraceStillGrantsIt() {
        projection("grace", null, NOW.plusHours(1));
        feature(true, null);

        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isTrue();
    }

    @Test
    void aSubscriptionThatIsNeitherActiveNorInGraceGrantsNothing() {
        projection("SUSPENDED", null, NOW.plusDays(1));

        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isFalse();
    }

    @Test
    void theSubscriptionEndsExactlyAtItsEffectiveUntil() {
        projection("ACTIVE", NOW, NOW.plusDays(1));

        // until is exclusive: at the instant it equals now the access is already over
        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isFalse();
    }

    @Test
    void aProjectionThatIsNotReconciledInTimeGrantsNothing() {
        projection("ACTIVE", null, NOW);

        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isFalse();
    }

    @Test
    void aDisabledOrExpiredFeatureGrantsNothing() {
        projection("ACTIVE", null, NOW.plusDays(1));
        when(featureRepository.findByUserIdAndFeatureKey("user-1", FEATURE)).thenReturn(Optional.of(
                UserFeatureEntitlement.builder().userId("user-1").featureKey(FEATURE).enabled(false).build()));
        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isFalse();

        when(featureRepository.findByUserIdAndFeatureKey("user-1", FEATURE)).thenReturn(Optional.of(
                UserFeatureEntitlement.builder().userId("user-1").featureKey(FEATURE).enabled(true).effectiveUntil(NOW).build()));
        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isFalse();
    }

    @Test
    void aUserWithoutAProjectionOrAFeatureKeyGrantsNothing() {
        when(projectionRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThat(adapter.hasFeature("user-1", FEATURE, NOW)).isFalse();
        assertThat(adapter.hasFeature("user-1", " ", NOW)).isFalse();
        assertThat(adapter.hasFeature("user-1", null, NOW)).isFalse();
        verifyNoInteractions(featureRepository);
    }

    private void projection(String status, LocalDateTime effectiveUntil, LocalDateTime reconcileBy) {
        when(projectionRepository.findById("user-1")).thenReturn(Optional.of(UserEntitlementProjection.builder()
                .userId("user-1").accessStatus(status).effectiveUntil(effectiveUntil).reconcileBy(reconcileBy).build()));
    }

    private void feature(boolean enabled, LocalDateTime effectiveUntil) {
        when(featureRepository.findByUserIdAndFeatureKey("user-1", FEATURE)).thenReturn(Optional.of(
                UserFeatureEntitlement.builder().userId("user-1").featureKey(FEATURE).enabled(enabled).effectiveUntil(effectiveUntil).build()));
    }
}
