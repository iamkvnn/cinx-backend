package com.cinx.commerce.subscription.application;

import com.cinx.commerce.catalog.application.PlanCatalogLookup;
import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.catalog.application.query.SaleableOffer;
import com.cinx.commerce.subscription.application.command.AdjustSubscriptionCommand;
import com.cinx.commerce.subscription.application.query.CheckoutSessionView;
import com.cinx.commerce.subscription.application.query.SubscriptionView;
import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CommerceSubscriptionRepository;
import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointer;
import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointerRepository;
import com.cinx.commerce.subscription.domain.SubscriptionAdjustment;
import com.cinx.commerce.subscription.domain.SubscriptionAdjustmentRepository;
import com.cinx.commerce.subscription.domain.SubscriptionCommand;
import com.cinx.commerce.subscription.domain.SubscriptionCommandRepository;
import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import com.cinx.commerce.subscription.domain.SubscriptionSubjectRepository;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    static final Instant NOW_INSTANT = Instant.parse("2026-10-07T03:00:00Z");
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock SubscriptionSubjectRepository subjects;
    @Mock CurrentSubscriptionPointerRepository pointers;
    @Mock SubscriptionCommandRepository commands;
    @Mock CommerceSubscriptionRepository subscriptions;
    @Mock PlanCatalogLookup plans;
    @Mock ObjectProvider<BillingProvider> billingProvider;
    @Mock BillingProvider provider;
    @Mock SubscriptionAdjustmentRepository adjustments;
    SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subjects, pointers, commands, subscriptions, plans, billingProvider,
                Clock.fixed(NOW_INSTANT, ZoneOffset.UTC), adjustments, new ObjectMapper());
        lenient().when(billingProvider.getIfAvailable()).thenReturn(provider);
        lenient().when(subjects.findOrCreate(any(SubscriptionSubject.class))).thenReturn(subject());
    }

    // ---- read

    @Test
    void aUserWithoutASubjectHasNoSubscription() {
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.empty());

        assertThat(service.read("user-1")).isNull();
    }

    @Test
    void theCurrentSubscriptionIsTheOneThePointerNamesWithItsPlansCode() {
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.find("s1", "BASE")).thenReturn(Optional.of(CurrentSubscriptionPointer.builder().subjectId("s1").subscriptionSlotCode("BASE").subscriptionId("sub-1").build()));
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(subscription("ACTIVE", "ACTIVE")));
        when(plans.planCodeOfVersion("pv1")).thenReturn(Optional.of("LEARNER_PLUS"));

        SubscriptionView view = service.read("user-1");

        assertThat(view.subscriptionId()).isEqualTo("sub-1");
        assertThat(view.planCode()).isEqualTo("LEARNER_PLUS");
        assertThat(view.billingStatus()).isEqualTo("ACTIVE");
        assertThat(view.entitlementVersion()).isEqualTo(3L);
    }

    @Test
    void withoutAPointerTheNewestSubscriptionOfTheSubjectIsUsedAndAnUnknownPlanFallsBackToTheVersionId() {
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.find("s1", "BASE")).thenReturn(Optional.empty());
        when(subscriptions.findAllBySubjectId("s1")).thenReturn(List.of(subscription("ACTIVE", "ACTIVE")));
        when(plans.planCodeOfVersion("pv1")).thenReturn(Optional.empty());

        assertThat(service.read("user-1").planCode()).isEqualTo("pv1");
    }

    // ---- checkout

    @Test
    void aFreeOfferCompletesTheCommandWithoutTouchingTheProvider() {
        givenOffer("o1", null);
        givenNewCommand();

        CheckoutSessionView view = service.createCheckout("user-1", "key-1", "o1", "/plans");

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.checkoutUrl()).isNull();
        verify(commands).save(any(SubscriptionCommand.class));
        verify(provider, never()).createSubscriptionCheckout(any());
    }

    @Test
    void aPaidOfferStartsAProviderCheckoutAndKeepsItsUrlOnTheCommand() {
        ReflectionTestUtils.setField(service, "billingEnabled", true);
        givenOffer("o1", "price-1");
        AtomicReference<SubscriptionCommand> created = givenNewCommand();
        Instant expires = NOW_INSTANT.plusSeconds(1800);
        when(provider.createSubscriptionCheckout(any())).thenReturn(
                new BillingProvider.CheckoutSession("sess-1", "https://pay.example/session", expires));

        CheckoutSessionView view = service.createCheckout("user-1", "key-1", "o1", "/plans");

        ArgumentCaptor<BillingProvider.CheckoutCommand> sent = ArgumentCaptor.forClass(BillingProvider.CheckoutCommand.class);
        verify(provider).createSubscriptionCheckout(sent.capture());
        assertThat(sent.getValue().providerPriceReference()).isEqualTo("price-1");
        assertThat(sent.getValue().customerReference()).isEqualTo("user-1");
        assertThat(sent.getValue().commandId()).isEqualTo(created.get().getCommandId());
        assertThat(view.checkoutUrl()).isEqualTo("https://pay.example/session");
        assertThat(view.expiresAt()).isEqualTo(expires);
        assertThat(view.status()).isEqualTo("PROVIDER_PENDING");
        assertThat(created.get().getResponseReference()).isEqualTo("https://pay.example/session");
    }

    @Test
    void aPaidOfferIsRefusedWhileBillingIsSwitchedOff() {
        givenOffer("o1", "price-1");
        givenNewCommand();

        assertThatThrownBy(() -> service.createCheckout("user-1", "key-1", "o1", "/plans"))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("billing is disabled");
        verify(provider, never()).createSubscriptionCheckout(any());
    }

    @Test
    void aRepeatedRequestReturnsTheStoredCheckoutAndDoesNotCreateAnother() {
        ReflectionTestUtils.setField(service, "billingEnabled", true);
        givenOffer("o1", "price-1");
        String fingerprint = IdempotencyFingerprint.sha256("user-1", "CREATE_CHECKOUT", "offer=o1&returnPath=/plans");
        SubscriptionCommand stored = SubscriptionCommand.builder().commandId("cmd-1").subjectId("s1").idempotencyKey("key-1")
                .requestFingerprint(fingerprint).status("PROVIDER_PENDING").responseReference("https://pay.example/stored").build();
        when(commands.findBySubjectIdAndIdempotencyKey("s1", "key-1")).thenReturn(Optional.of(stored));

        CheckoutSessionView view = service.createCheckout("user-1", "key-1", "o1", "/plans");

        assertThat(view.checkoutUrl()).isEqualTo("https://pay.example/stored");
        assertThat(view.commandId()).isEqualTo("cmd-1");
        verify(provider, never()).createSubscriptionCheckout(any());
        verify(commands, never()).insertIfAbsent(any());
    }

    @Test
    void theSameKeyForAnotherRequestIsRefused() {
        givenOffer("o1", "price-1");
        SubscriptionCommand stored = SubscriptionCommand.builder().commandId("cmd-1").subjectId("s1").idempotencyKey("key-1")
                .requestFingerprint("another-request").status("PENDING").build();
        when(commands.findBySubjectIdAndIdempotencyKey("s1", "key-1")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.createCheckout("user-1", "key-1", "o1", "/plans"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Idempotency-Key");
    }

    @Test
    void whenTwoRequestsRaceTheLoserGetsTheWinnersCommand() {
        givenOffer("o1", null);
        String fingerprint = IdempotencyFingerprint.sha256("user-1", "CREATE_CHECKOUT", "offer=o1&returnPath=/plans");
        SubscriptionCommand winner = SubscriptionCommand.builder().commandId("cmd-win").subjectId("s1").idempotencyKey("key-1")
                .requestFingerprint(fingerprint).status("COMPLETED").responseReference("done").build();
        when(commands.findBySubjectIdAndIdempotencyKey("s1", "key-1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(commands.insertIfAbsent(any())).thenReturn(Optional.empty());

        CheckoutSessionView view = service.createCheckout("user-1", "key-1", "o1", "/plans");

        assertThat(view.commandId()).isEqualTo("cmd-win");
    }

    @Test
    void theReturnPathMustStayOnTheSiteAndTheKeyIsRequired() {
        givenOffer("o1", null);

        for (String path : new String[]{null, "https://evil.example", "//evil.example", "/a\\b", "/a\nb"}) {
            assertThatThrownBy(() -> service.createCheckout("user-1", "key-1", "o1", path)).as(String.valueOf(path))
                    .isInstanceOf(ForbiddenException.class).hasMessageContaining("Return path");
        }
        for (String key : new String[]{null, " ", "k".repeat(256)}) {
            assertThatThrownBy(() -> service.createCheckout("user-1", key, "o1", "/plans")).isInstanceOf(BadRequestException.class);
        }
    }

    // ---- cancel, change, resume

    @Test
    void cancellingAskstheProviderForTheEndOfThePeriodAndMarksTheSubscription() {
        ReflectionTestUtils.setField(service, "billingEnabled", true);
        CommerceSubscription current = givenCurrent("ACTIVE", "ACTIVE");
        current.setProviderSubscriptionRef("sub_ref");
        AtomicReference<SubscriptionCommand> created = givenNewCommand();
        Instant periodEnd = Instant.parse("2026-11-01T00:00:00Z");
        when(provider.cancelAtPeriodEnd(anyString(), anyString())).thenReturn(
                new BillingProvider.ProviderSubscription("sub_ref", "active", NOW_INSTANT, periodEnd, true, 7L));

        service.cancel("user-1", "key-1");

        assertThat(current.isCancelAtPeriodEnd()).isTrue();
        assertThat(current.getCurrentPeriodEnd()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(current.getProviderObjectVersion()).isEqualTo(7L);
        assertThat(created.get().getStatus()).isEqualTo("COMPLETED");
        verify(subscriptions).save(current);
    }

    @Test
    void aSubscriptionThatIsEndedCannotBeCancelled() {
        givenCurrent("ENDED", "ENDED");

        assertThatThrownBy(() -> service.cancel("user-1", "key-1")).isInstanceOf(ConflictException.class);
        verify(commands, never()).insertIfAbsent(any());
    }

    @Test
    void cancellingWithNoSubscriptionIsAConflict() {
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel("user-1", "key-1")).isInstanceOf(ConflictException.class).hasMessageContaining("No current subscription");
    }

    @Test
    void aCancelThatAlreadyCompletedUnderTheSameKeyDoesNothingMore() {
        CommerceSubscription current = givenCurrent("ACTIVE", "ACTIVE");
        String fingerprint = IdempotencyFingerprint.sha256("user-1", "CANCEL", "sub-1");
        when(commands.findBySubjectIdAndIdempotencyKey("s1", "key-1")).thenReturn(Optional.of(SubscriptionCommand.builder()
                .commandId("cmd-1").requestFingerprint(fingerprint).status("COMPLETED").build()));

        service.cancel("user-1", "key-1");

        assertThat(current.isCancelAtPeriodEnd()).isFalse();
        verify(subscriptions, never()).save(any());
    }

    @Test
    void changingAPlanNeedsBillingAndAPaidOffer() {
        givenOffer("o1", "price-1");
        givenCurrent("ACTIVE", "ACTIVE");
        givenNewCommand();

        assertThatThrownBy(() -> service.change("user-1", "key-1", "o1")).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("require an enabled billing provider");
        verify(provider, never()).changeSubscription(any());
    }

    @Test
    void resumingClearsTheCancellationAndAsksTheProvider() {
        ReflectionTestUtils.setField(service, "billingEnabled", true);
        CommerceSubscription current = givenCurrent("ACTIVE", "ACTIVE");
        current.setCancelAtPeriodEnd(true);
        current.setProviderSubscriptionRef("sub_ref");
        givenNewCommand();
        when(provider.resume(anyString(), anyString())).thenReturn(
                new BillingProvider.ProviderSubscription("sub_ref", "active", NOW_INSTANT, null, false, 9L));

        service.resume("user-1", "key-1");

        assertThat(current.isCancelAtPeriodEnd()).isFalse();
        assertThat(current.getProviderObjectVersion()).isEqualTo(9L);
    }

    // ---- staff adjustment

    @Test
    void anAdjustmentIsRecordedAndMovesTheSubscriptionsVersionOnByOne() {
        CommerceSubscription subscription = subscription("ACTIVE", "ACTIVE");
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(subscription));
        when(subjects.findById("s1")).thenReturn(Optional.of(subject()));
        AtomicReference<SubscriptionCommand> created = givenNewCommand();

        SubscriptionView view = service.adjust("sub-1", "key-1", new AdjustSubscriptionCommand("a.quota", Map.of("limit", 500),
                "goodwill", NOW, NOW.plusDays(30)), "staff-7");

        ArgumentCaptor<SubscriptionAdjustment> saved = ArgumentCaptor.forClass(SubscriptionAdjustment.class);
        verify(adjustments).save(saved.capture());
        assertThat(saved.getValue().getSubscriptionId()).isEqualTo("sub-1");
        assertThat(saved.getValue().getFeatureKey()).isEqualTo("a.quota");
        assertThat(saved.getValue().getValueJson()).isEqualTo("{\"limit\":500}");
        assertThat(saved.getValue().getCreatedBy()).isEqualTo("staff-7");
        assertThat(subscription.getAggregateVersion()).isEqualTo(4L); // was 3
        assertThat(view.entitlementVersion()).isEqualTo(4L);
        assertThat(created.get().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void anAdjustmentThatAlreadyCompletedIsNotAppliedTwice() {
        CommerceSubscription subscription = subscription("ACTIVE", "ACTIVE");
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(subscription));
        when(subjects.findById("s1")).thenReturn(Optional.of(subject()));
        AdjustSubscriptionCommand request = new AdjustSubscriptionCommand("a.quota", Map.of("limit", 500), "goodwill", NOW, null);
        String fingerprint = IdempotencyFingerprint.sha256("user-1", "ADJUST",
                "sub-1|a.quota|{limit=500}|goodwill|" + NOW + "|null");
        when(commands.findBySubjectIdAndIdempotencyKey("s1", "key-1")).thenReturn(Optional.of(SubscriptionCommand.builder()
                .commandId("cmd-1").requestFingerprint(fingerprint).status("COMPLETED").build()));

        service.adjust("sub-1", "key-1", request, "staff-7");

        verify(adjustments, never()).save(any());
        assertThat(subscription.getAggregateVersion()).isEqualTo(3L);
    }

    @Test
    void theEntitlementSnapshotOfAUserWithNothingIsInactiveAndEmpty() {
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.empty());

        Map<String, Object> snapshot = service.entitlementSnapshot("user-1");

        assertThat(snapshot).containsEntry("accessStatus", "INACTIVE").containsEntry("entitlementVersion", 0L)
                .containsEntry("userId", "user-1").containsEntry("subjectId", "user-1");
        assertThat((List<?>) snapshot.get("features")).isEmpty();
    }

    @Test
    void theEntitlementSnapshotOfASubscriberListsWhatTheirPlanGrants() {
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.find("s1", "BASE")).thenReturn(Optional.of(CurrentSubscriptionPointer.builder().subjectId("s1").subscriptionSlotCode("BASE").subscriptionId("sub-1").build()));
        CommerceSubscription subscription = subscription("ACTIVE", "ACTIVE");
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(subscription));
        when(subscriptions.findAllBySubjectId("s1")).thenReturn(List.of(subscription));
        when(plans.planCodeOfVersion("pv1")).thenReturn(Optional.of("LEARNER_PLUS"));
        when(plans.grantsOf("pv1")).thenReturn(List.of(
                new PlanFeatureGrant("a.on", "BOOLEAN", true, null, null, null, null),
                new PlanFeatureGrant("b.zero", "QUOTA", null, 0L, false, "REQUEST", "CALENDAR_MONTH"),
                new PlanFeatureGrant("c.unlimited", "QUOTA", null, null, true, "REQUEST", "CALENDAR_MONTH")));

        Map<String, Object> snapshot = service.entitlementSnapshot("user-1");

        assertThat(snapshot).containsEntry("accessStatus", "ACTIVE").containsEntry("entitlementVersion", 3L)
                .containsEntry("subscriptionId", "sub-1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features = (List<Map<String, Object>>) snapshot.get("features");
        assertThat(features).extracting(feature -> feature.get("key"), feature -> feature.get("enabled"))
                .containsExactly(org.assertj.core.groups.Tuple.tuple("a.on", true), org.assertj.core.groups.Tuple.tuple("b.zero", false),
                        org.assertj.core.groups.Tuple.tuple("c.unlimited", true));
    }

    // ---- fixtures

    private void givenOffer(String offerId, String priceId) {
        when(plans.resolveSaleableOffer(offerId, NOW)).thenReturn(new SaleableOffer(offerId, priceId));
    }

    /** A command the store accepts: the lookup finds none and the insert keeps the one the service built. */
    private AtomicReference<SubscriptionCommand> givenNewCommand() {
        AtomicReference<SubscriptionCommand> created = new AtomicReference<>();
        when(commands.findBySubjectIdAndIdempotencyKey("s1", "key-1")).thenReturn(Optional.empty());
        when(commands.insertIfAbsent(any(SubscriptionCommand.class))).thenAnswer(invocation -> {
            created.set(invocation.getArgument(0));
            return Optional.of(created.get());
        });
        lenient().when(commands.save(any(SubscriptionCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return created;
    }

    private CommerceSubscription givenCurrent(String billingStatus, String accessStatus) {
        CommerceSubscription subscription = subscription(billingStatus, accessStatus);
        when(subjects.findByTypeAndReference("USER", "user-1")).thenReturn(Optional.of(subject()));
        when(pointers.find("s1", "BASE")).thenReturn(Optional.of(CurrentSubscriptionPointer.builder().subjectId("s1").subscriptionSlotCode("BASE").subscriptionId("sub-1").build()));
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(subscription));
        when(plans.planCodeOfVersion("pv1")).thenReturn(Optional.of("LEARNER_PLUS"));
        return subscription;
    }

    private static SubscriptionSubject subject() {
        return SubscriptionSubject.builder().id("s1").subjectType("USER").subjectReference("user-1").createdAt(NOW).build();
    }

    private static CommerceSubscription subscription(String billingStatus, String accessStatus) {
        return CommerceSubscription.builder().id("sub-1").subjectId("s1").planVersionId("pv1").billingStatus(billingStatus)
                .accessStatus(accessStatus).currentPeriodStart(NOW.minusDays(10)).currentPeriodEnd(NOW.plusDays(20))
                .aggregateVersion(3L).build();
    }
}
