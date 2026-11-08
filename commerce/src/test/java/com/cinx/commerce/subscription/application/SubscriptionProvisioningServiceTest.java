package com.cinx.commerce.subscription.application;

import com.cinx.commerce.catalog.application.PlanCatalogLookup;
import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.catalog.application.query.PlanVersionRef;
import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CommerceSubscriptionRepository;
import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointer;
import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointerRepository;
import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import com.cinx.commerce.subscription.domain.SubscriptionSubjectRepository;
import com.cinx.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionProvisioningServiceTest {
    // 2026-10-07 is in October, so a free subscription runs to the first day of November
    static final Instant NOW_INSTANT = Instant.parse("2026-10-07T03:00:00Z");
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);
    static final PlanVersionRef FREE = new PlanVersionRef("free-v1", "free-plan", "LEARNER_FREE", 1);

    @Mock SubscriptionSubjectRepository subjects;
    @Mock CurrentSubscriptionPointerRepository pointers;
    @Mock CommerceSubscriptionRepository subscriptions;
    @Mock PlanCatalogLookup plans;
    @Mock EntitlementSnapshots snapshots;
    @Mock ObjectProvider<BillingProvider> billingProvider;
    @Mock BillingProvider provider;
    SubscriptionProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionProvisioningService(subjects, pointers, subscriptions, plans, snapshots, billingProvider,
                Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));
        lenient().when(billingProvider.getIfAvailable()).thenReturn(provider);
        lenient().when(subscriptions.save(any(CommerceSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aNewLearnerGetsTheFreePlanUntilTheEndOfTheMonthAndTheOtherServicesAreTold() {
        givenSubject();
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.empty());
        when(plans.findVersion("LEARNER_FREE", 1)).thenReturn(Optional.of(FREE));
        when(plans.grantsOf("free-v1")).thenReturn(List.of(new PlanFeatureGrant("a.on", "BOOLEAN", true, null, null, null, null)));

        CommerceSubscription created = service.ensureFreeSubscription("user-1", "USER_PROFILE_ONBOARDED");

        assertThat(created.getPlanVersionId()).isEqualTo("free-v1");
        assertThat(created.getOrigin()).isEqualTo("INTERNAL_FREE");
        assertThat(created.getBillingStatus()).isEqualTo("ACTIVE");
        assertThat(created.getAccessStatus()).isEqualTo("ACTIVE");
        assertThat(created.getCurrentPeriodStart()).isEqualTo(NOW);
        assertThat(created.getCurrentPeriodEnd()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(created.getAggregateVersion()).isEqualTo(1L);
        ArgumentCaptor<CurrentSubscriptionPointer> pointer = ArgumentCaptor.forClass(CurrentSubscriptionPointer.class);
        verify(pointers).save(pointer.capture());
        assertThat(pointer.getValue().getSubscriptionId()).isEqualTo(created.getId());
        assertThat(pointer.getValue().getSubscriptionSlotCode()).isEqualTo("BASE");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> snapshot = ArgumentCaptor.forClass(Map.class);
        verify(snapshots).publish(eq("s1"), snapshot.capture());
        assertThat(snapshot.getValue()).containsEntry("userId", "user-1").containsEntry("accessStatus", "ACTIVE").containsEntry("entitlementVersion", 1L);
        assertThat((List<?>) snapshot.getValue().get("features")).hasSize(1);
    }

    @Test
    void aLearnerWhoAlreadyHasASubscriptionKeepsIt() {
        givenSubject();
        CommerceSubscription existing = subscription("sub-1", "paid-v1");
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.of(pointer("sub-1")));
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(existing));

        assertThat(service.ensureFreeSubscription("user-1", "again")).isSameAs(existing);

        verify(pointers, never()).save(any());
        verify(snapshots, never()).publish(any(), any());
    }

    @Test
    void aPointerToASubscriptionThatIsGoneIsReportedNotRepairedSilently() {
        givenSubject();
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.of(pointer("missing")));
        when(subscriptions.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensureFreeSubscription("user-1", "x")).isInstanceOf(ConflictException.class)
                .hasMessageContaining("pointer is broken");
    }

    @Test
    void aBlankUserIsRefused() {
        assertThatThrownBy(() -> service.ensureFreeSubscription(" ", "x")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.ensureFreeSubscription(null, "x")).isInstanceOf(ConflictException.class);
    }

    @Test
    void aMissingFreePlanIsReportedWithTheReasonThatTellsThemWhatToFix() {
        givenSubject();
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.empty());
        when(plans.findVersion("LEARNER_FREE", 1)).thenReturn(Optional.empty());
        when(plans.planExists("LEARNER_FREE")).thenReturn(false, true);

        assertThatThrownBy(() -> service.ensureFreeSubscription("user-1", "x")).hasMessageContaining("bootstrap is unavailable");
        assertThatThrownBy(() -> service.ensureFreeSubscription("user-1", "x")).hasMessageContaining("version is unavailable");
    }

    @Test
    void losingEligibilityRevokesThePaidSubscriptionAtOnceAndSchedulesTheProviderCancellation() {
        ReflectionTestUtils.setField(service, "billingEnabled", true);
        CommerceSubscription paid = subscription("sub-paid", "paid-v1");
        paid.setProviderSubscriptionRef("sub_ref");
        paid.setAggregateVersion(5L);
        when(subjects.lockByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.of(pointer("sub-paid")));
        when(subscriptions.findById("sub-paid")).thenReturn(Optional.of(paid));
        when(plans.findVersion("LEARNER_FREE", 1)).thenReturn(Optional.of(FREE));

        CommerceSubscription free = service.fallbackToFreeSubscription("user-1", "AUTHORIZATION_ELIGIBILITY_LOSS");

        assertThat(paid.getAccessStatus()).isEqualTo("INELIGIBLE");
        assertThat(paid.getBillingStatus()).isEqualTo("INELIGIBLE");
        assertThat(paid.getAggregateVersion()).isEqualTo(6L);
        verify(provider).cancelAtPeriodEnd("sub_ref", "eligibility-loss-sub-paid");
        assertThat(free.getPlanVersionId()).isEqualTo("free-v1");
        assertThat(free.getAggregateVersion()).isEqualTo(7L); // one past the revoked paid subscription
        ArgumentCaptor<CurrentSubscriptionPointer> pointer = ArgumentCaptor.forClass(CurrentSubscriptionPointer.class);
        verify(pointers).save(pointer.capture());
        assertThat(pointer.getValue().getSubscriptionId()).isEqualTo(free.getId());
        verify(snapshots).publish(eq("s1"), any());
    }

    @Test
    void aProviderFailureWhileSchedulingTheCancellationDoesNotReopenAccess() {
        ReflectionTestUtils.setField(service, "billingEnabled", true);
        CommerceSubscription paid = subscription("sub-paid", "paid-v1");
        paid.setProviderSubscriptionRef("sub_ref");
        when(subjects.lockByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.of(pointer("sub-paid")));
        when(subscriptions.findById("sub-paid")).thenReturn(Optional.of(paid));
        when(plans.findVersion("LEARNER_FREE", 1)).thenReturn(Optional.of(FREE));
        when(provider.cancelAtPeriodEnd(anyString(), anyString())).thenThrow(new IllegalStateException("provider down"));

        service.fallbackToFreeSubscription("user-1", "x");

        assertThat(paid.getAccessStatus()).isEqualTo("INELIGIBLE");
    }

    @Test
    void aLearnerAlreadyOnTheFreePlanIsLeftAlone() {
        CommerceSubscription onFree = subscription("sub-free", "free-v1");
        when(subjects.lockByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.of(pointer("sub-free")));
        when(subscriptions.findById("sub-free")).thenReturn(Optional.of(onFree));
        when(plans.findVersion("LEARNER_FREE", 1)).thenReturn(Optional.of(FREE));

        assertThat(service.fallbackToFreeSubscription("user-1", "x")).isSameAs(onFree);

        verify(subscriptions, never()).save(any());
        verify(snapshots, never()).publish(any(), any());
    }

    @Test
    void anActiveLearnerAccountGetsTheFreePlanAnActiveNonLearnerFallsBackAndADisabledAccountLosesAccess() {
        // active learner: ensure
        givenSubject();
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.of(pointer("sub-1")));
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(subscription("sub-1", "free-v1")));
        service.applyAuthorization("user-1", "ACTIVE", List.of("LEARNER"));
        verify(subjects).lockOrCreate(any(SubscriptionSubject.class));

        // disabled account: the current subscription loses access and the change is announced without features
        CommerceSubscription current = subscription("sub-1", "free-v1");
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(current));
        service.applyAuthorization("user-1", "BANNED", List.of("LEARNER"));
        assertThat(current.getAccessStatus()).isEqualTo("INELIGIBLE");
        assertThat(current.getAggregateVersion()).isEqualTo(2L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> snapshot = ArgumentCaptor.forClass(Map.class);
        verify(snapshots).publish(eq("s1"), snapshot.capture());
        assertThat((List<?>) snapshot.getValue().get("features")).isEmpty();
    }

    @Test
    void aDisabledAccountWhoseSubscriptionIsAlreadyIneligibleChangesNothing() {
        CommerceSubscription current = subscription("sub-1", "free-v1");
        current.setAccessStatus("INELIGIBLE");
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.lock("s1", "BASE")).thenReturn(Optional.of(pointer("sub-1")));
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(current));

        service.applyAuthorization("user-1", "BANNED", List.of());

        verify(subscriptions, never()).save(any());
        verify(snapshots, never()).publish(any(), any());
    }

    private void givenSubject() {
        when(subjects.lockOrCreate(any(SubscriptionSubject.class))).thenReturn(subject());
    }

    private static SubscriptionSubject subject() {
        return SubscriptionSubject.builder().id("s1").subjectType("USER").subjectReference("user-1").createdAt(NOW).build();
    }

    private static CurrentSubscriptionPointer pointer(String subscriptionId) {
        return CurrentSubscriptionPointer.builder().subjectId("s1").subscriptionSlotCode("BASE").subscriptionId(subscriptionId).build();
    }

    private static CommerceSubscription subscription(String id, String planVersionId) {
        return CommerceSubscription.builder().id(id).subjectId("s1").planVersionId(planVersionId).billingStatus("ACTIVE")
                .accessStatus("ACTIVE").currentPeriodStart(NOW.minusDays(5)).currentPeriodEnd(NOW.plusDays(20))
                .aggregateVersion(1L).createdAt(NOW.minusDays(5)).build();
    }
}
