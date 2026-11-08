package com.cinx.commerce.subscription.application;

import com.cinx.commerce.catalog.application.PlanCatalogLookup;
import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.subscription.domain.BillingWebhookInbox;
import com.cinx.commerce.subscription.domain.BillingWebhookInboxRepository;
import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CommerceSubscriptionRepository;
import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import com.cinx.commerce.subscription.domain.SubscriptionSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingWebhookServiceTest {
    static final Instant NOW_INSTANT = Instant.parse("2026-10-07T03:00:00Z");
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);
    static final byte[] BODY = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @Mock BillingWebhookInboxRepository inbox;
    @Mock CommerceSubscriptionRepository subscriptions;
    @Mock SubscriptionSubjectRepository subjects;
    @Mock AuthEligibility eligibility;
    @Mock EntitlementSnapshots snapshots;
    @Mock PlanCatalogLookup plans;
    BillingWebhookService service;

    @BeforeEach
    void setUp() {
        service = new BillingWebhookService(inbox, subscriptions, subjects, eligibility, snapshots, plans, Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));
    }

    @Test
    void aRepeatedEventIsIgnoredWithoutRecordingAnything() {
        when(inbox.exists("STRIPE", "default", "evt-1")).thenReturn(true);

        service.accept("stripe", "default", BODY, event("evt-1", 5, "customer.subscription.updated", "active"));

        verify(inbox, never()).insertIfAbsent(any());
        verify(subscriptions, never()).save(any());
    }

    @Test
    void anEventAnotherRequestJustRecordedIsIgnored() {
        when(inbox.exists("STRIPE", "default", "evt-1")).thenReturn(false);
        when(inbox.insertIfAbsent(any())).thenReturn(false);

        service.accept("stripe", "default", BODY, event("evt-1", 5, "customer.subscription.updated", "active"));

        verify(subscriptions, never()).findByProviderRef(any(), any(), any());
        verify(inbox, never()).save(any());
    }

    @Test
    void anEventForASubscriptionWeDoNotKnowIsRecordedAsProcessed() {
        givenNewEvent();
        when(subscriptions.findByProviderRef("STRIPE", "default", "sub_ref")).thenReturn(Optional.empty());

        service.accept("stripe", "default", BODY, event("evt-1", 5, "customer.subscription.updated", "active"));

        BillingWebhookInbox saved = savedInbox();
        assertThat(saved.getProcessingStatus()).isEqualTo("PROCESSED");
        assertThat(saved.getProcessedAt()).isEqualTo(NOW);
        // SHA-256 of "{}", from `printf '{}' | shasum -a 256`
        assertThat(saved.getPayloadHash()).isEqualTo("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");
        verify(subscriptions, never()).save(any());
    }

    @Test
    void anEventOlderThanWhatWeAlreadyHaveChangesNothing() {
        givenNewEvent();
        CommerceSubscription current = subscription("ACTIVE", "ACTIVE", 5);
        when(subscriptions.findByProviderRef("STRIPE", "default", "sub_ref")).thenReturn(Optional.of(current));

        service.accept("stripe", "default", BODY, event("evt-1", 5, "customer.subscription.updated", "past_due"));

        assertThat(current.getBillingStatus()).isEqualTo("ACTIVE");
        verify(subscriptions, never()).save(any());
        verify(snapshots, never()).publish(any(), any());
    }

    @Test
    void aTransitionTheLifecycleForbidsIsRecordedAsInvalidAndNotApplied() {
        givenNewEvent();
        CommerceSubscription ended = subscription("ENDED", "ENDED", 1);
        when(subscriptions.findByProviderRef("STRIPE", "default", "sub_ref")).thenReturn(Optional.of(ended));

        service.accept("stripe", "default", BODY, event("evt-1", 2, "customer.subscription.updated", "active"));

        BillingWebhookInbox saved = savedInbox();
        assertThat(saved.getProcessingStatus()).isEqualTo("INVALID_TRANSITION");
        assertThat(saved.getFailureCode()).isEqualTo("SUBSCRIPTION_TRANSITION_INVALID");
        assertThat(ended.getBillingStatus()).isEqualTo("ENDED");
        verify(snapshots, never()).publish(any(), any());
    }

    @Test
    void aLegalEventUpdatesTheSubscriptionAndAnnouncesTheNewEntitlements() {
        givenNewEvent();
        CommerceSubscription current = subscription("PENDING", "PENDING", 1);
        when(subscriptions.findByProviderRef("STRIPE", "default", "sub_ref")).thenReturn(Optional.of(current));
        when(subjects.findById("s1")).thenReturn(Optional.of(SubscriptionSubject.builder().id("s1").subjectReference("user-1").build()));
        when(eligibility.isActive("user-1")).thenReturn(true);
        when(plans.grantsOf("pv1")).thenReturn(List.of(new PlanFeatureGrant("a.on", "BOOLEAN", true, null, null, null, null)));

        service.accept("stripe", "default", BODY, event("evt-1", 4, "customer.subscription.updated", "active"));

        assertThat(current.getBillingStatus()).isEqualTo("ACTIVE");
        assertThat(current.getAccessStatus()).isEqualTo("ACTIVE");
        assertThat(current.getProviderObjectVersion()).isEqualTo(4L);
        assertThat(current.getAggregateVersion()).isEqualTo(3L);
        verify(subscriptions).save(current);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> snapshot = ArgumentCaptor.forClass(Map.class);
        verify(snapshots).publish(eq("s1"), snapshot.capture());
        assertThat(snapshot.getValue()).containsEntry("userId", "user-1").containsEntry("accessStatus", "ACTIVE");
        assertThat(savedInbox().getProcessingStatus()).isEqualTo("PROCESSED");
    }

    @Test
    void aPaidEventNeverGivesAccessToAnAccountTheAuthServiceDoesNotSayIsActive() {
        givenNewEvent();
        CommerceSubscription current = subscription("PENDING", "PENDING", 1);
        when(subscriptions.findByProviderRef("STRIPE", "default", "sub_ref")).thenReturn(Optional.of(current));
        when(subjects.findById("s1")).thenReturn(Optional.of(SubscriptionSubject.builder().id("s1").subjectReference("user-1").build()));
        when(eligibility.isActive("user-1")).thenReturn(false);

        service.accept("stripe", "default", BODY, event("evt-1", 4, "customer.subscription.updated", "active"));

        // the provider's status is kept for reconciliation, the local access fails closed
        assertThat(current.getBillingStatus()).isEqualTo("ACTIVE");
        assertThat(current.getAccessStatus()).isEqualTo("INELIGIBLE");
    }

    @Test
    void aFailedPaymentMovesTheSubscriptionToPastDueAndRevokesAccess() {
        givenNewEvent();
        CommerceSubscription current = subscription("ACTIVE", "ACTIVE", 1);
        when(subscriptions.findByProviderRef("STRIPE", "default", "sub_ref")).thenReturn(Optional.of(current));
        when(subjects.findById("s1")).thenReturn(Optional.of(SubscriptionSubject.builder().id("s1").subjectReference("user-1").build()));
        when(eligibility.isActive("user-1")).thenReturn(true);

        service.accept("stripe", "default", BODY, event("evt-1", 2, "invoice.payment_failed", "ignored"));

        assertThat(current.getBillingStatus()).isEqualTo("PAST_DUE");
        assertThat(current.getAccessStatus()).isEqualTo("INELIGIBLE");
    }

    private void givenNewEvent() {
        when(inbox.exists("STRIPE", "default", "evt-1")).thenReturn(false);
        when(inbox.insertIfAbsent(any())).thenReturn(true);
    }

    private BillingWebhookInbox savedInbox() {
        ArgumentCaptor<BillingWebhookInbox> saved = ArgumentCaptor.forClass(BillingWebhookInbox.class);
        verify(inbox, atLeastOnce()).save(saved.capture());
        return saved.getValue();
    }

    private static BillingProvider.NormalizedWebhook event(String id, long version, String type, String status) {
        return new BillingProvider.NormalizedWebhook(id, "sub_ref", version, type, status, NOW_INSTANT);
    }

    private static CommerceSubscription subscription(String billingStatus, String accessStatus, long providerVersion) {
        return CommerceSubscription.builder().id("sub-1").subjectId("s1").planVersionId("pv1").billingStatus(billingStatus)
                .accessStatus(accessStatus).providerObjectVersion(providerVersion).aggregateVersion(2L)
                .currentPeriodStart(NOW.minusDays(1)).currentPeriodEnd(NOW.plusDays(29)).build();
    }
}
