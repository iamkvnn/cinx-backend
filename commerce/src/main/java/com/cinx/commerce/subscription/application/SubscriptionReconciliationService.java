package com.cinx.commerce.subscription.application;

import com.cinx.commerce.catalog.application.PlanCatalogLookup;
import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CommerceSubscriptionRepository;
import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import com.cinx.commerce.subscription.domain.SubscriptionSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Authenticated provider reconciliation for the interval between webhooks.
 * It is deliberately disabled while billing is disabled, and only observes
 * provider state through the BillingProvider port.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cinx.subscription.billing-enabled", havingValue = "true")
public class SubscriptionReconciliationService {
    private final CommerceSubscriptionRepository subscriptions;
    private final SubscriptionSubjectRepository subjects;
    private final AuthEligibility eligibility;
    private final PlanCatalogLookup plans;
    private final ObjectProvider<BillingProvider> provider;
    private final EntitlementSnapshots snapshotPublisher;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${cinx.subscription.reconciliation-interval-ms:60000}")
    @Transactional
    public void reconcile() {
        BillingProvider billingProvider = provider.getIfAvailable();
        if (billingProvider == null) return;
        for (CommerceSubscription subscription : subscriptions.findReconcilable()) {
            try {
                BillingProvider.ProviderSubscription remote = billingProvider.fetchSubscription(
                        subscription.getProviderSubscriptionRef());
                if (remote == null || (remote.providerVersion() > 0
                        && remote.providerVersion() <= subscription.getProviderObjectVersion())) {
                    continue;
                }
                if (remote.providerVersion() > 0) {
                    subscription.setProviderObjectVersion(remote.providerVersion());
                }
                subscription.setBillingStatus(normalizeBillingStatus(remote.status()));
                subscription.setAccessStatus(eligible(subscription)
                        ? normalizeAccessStatus(remote.status()) : "INELIGIBLE");
                if (remote.periodStart() != null) {
                    subscription.setCurrentPeriodStart(LocalDateTime.ofInstant(remote.periodStart(), ZoneOffset.UTC));
                }
                if (remote.periodEnd() != null) {
                    subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(remote.periodEnd(), ZoneOffset.UTC));
                }
                subscription.setCancelAtPeriodEnd(remote.cancelAtPeriodEnd());
                subscription.advanceVersion();
                subscription.setUpdatedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
                subscriptions.save(subscription);
                java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
                snapshot.put("subjectType", "USER");
                snapshot.put("subjectId", subscription.getSubjectId());
                snapshot.put("userId", subjects.findById(subscription.getSubjectId())
                        .map(SubscriptionSubject::getSubjectReference).orElse(subscription.getSubjectId()));
                snapshot.put("entitlementVersion", subscription.getAggregateVersion());
                snapshot.put("accessStatus", subscription.getAccessStatus());
                snapshot.put("effectiveFrom", clock.instant());
                snapshot.put("accessEffectiveUntil", subscription.getCurrentPeriodEnd() == null
                        ? null : subscription.getCurrentPeriodEnd().toInstant(ZoneOffset.UTC));
                snapshot.put("reconcileBy", clock.instant().plusSeconds(300));
                snapshot.put("currentPeriodStart", subscription.getCurrentPeriodStart() == null
                        ? null : subscription.getCurrentPeriodStart().toInstant(ZoneOffset.UTC));
                snapshot.put("currentPeriodEnd", subscription.getCurrentPeriodEnd() == null
                        ? null : subscription.getCurrentPeriodEnd().toInstant(ZoneOffset.UTC));
                snapshot.put("sources", java.util.List.of(java.util.Map.of(
                        "subscriptionId", subscription.getId(),
                        "subscriptionAggregateVersion", subscription.getAggregateVersion(),
                        "subscriptionSlot", "BASE",
                        "productLine", "DEFAULT_USER_BASE",
                        "planCode", subscription.getPlanVersionId(),
                        "planVersion", 1L)));
                java.time.Instant periodStart = subscription.getCurrentPeriodStart() == null ? null
                        : subscription.getCurrentPeriodStart().toInstant(ZoneOffset.UTC);
                java.time.Instant periodEnd = subscription.getCurrentPeriodEnd() == null ? null
                        : subscription.getCurrentPeriodEnd().toInstant(ZoneOffset.UTC);
                snapshot.put("features", plans.grantsOf(subscription.getPlanVersionId()).stream().map(feature -> {
                    java.util.Map<String, Object> value = new java.util.LinkedHashMap<>();
                    value.put("key", feature.featureKey());
                    value.put("type", feature.grantType());
                    value.put("enabled", "BOOLEAN".equalsIgnoreCase(feature.grantType())
                            ? Boolean.TRUE.equals(feature.booleanValue())
                            : Boolean.TRUE.equals(feature.quotaUnlimited())
                            || (feature.quotaLimit() != null && feature.quotaLimit() > 0));
                    value.put("limit", feature.quotaLimit());
                    value.put("unlimited", feature.quotaUnlimited());
                    value.put("unit", feature.quotaUnit());
                    value.put("resetPolicy", feature.resetPolicy());
                    value.put("periodStart", periodStart);
                    value.put("periodEnd", periodEnd);
                    value.put("effectiveUntil", periodEnd);
                    return value;
                }).toList());
                snapshot.put("occurredAt", clock.instant());
                snapshotPublisher.publish(subscription.getSubjectId(), snapshot);
            } catch (RuntimeException exception) {
                // A single provider/API failure must not prevent the bounded
                // worker from reconciling other subjects on the next pass.
                log.warn("Subscription reconciliation failed subscriptionId={}", subscription.getId(), exception);
            }
        }
    }

    private boolean eligible(CommerceSubscription subscription) {
        return subjects.findById(subscription.getSubjectId())
                .map(subject -> eligibility.isActive(subject.getSubjectReference()))
                .orElse(false);
    }

    private String normalizeBillingStatus(String status) {
        if (status == null || status.isBlank()) return "PENDING";
        return switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "active" -> "ACTIVE";
            case "trialing" -> "TRIALING";
            case "past_due" -> "PAST_DUE";
            case "canceled", "cancelled" -> "CANCELED";
            case "incomplete", "incomplete_expired" -> "PENDING";
            default -> status.toUpperCase(java.util.Locale.ROOT);
        };
    }

    private String normalizeAccessStatus(String status) {
        if (status == null) return "INELIGIBLE";
        return switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "active", "trialing" -> "ACTIVE";
            case "past_due" -> "GRACE";
            default -> "INELIGIBLE";
        };
    }
}
