package com.cinx.commerce.subscription.application;

import com.cinx.commerce.catalog.application.PlanCatalogLookup;
import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.subscription.domain.BillingWebhookInbox;
import com.cinx.commerce.subscription.domain.BillingWebhookInboxRepository;
import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CommerceSubscriptionRepository;
import com.cinx.commerce.subscription.domain.SubscriptionBillingStatus;
import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import com.cinx.commerce.subscription.domain.SubscriptionSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BillingWebhookService {
    private final BillingWebhookInboxRepository inbox;
    private final CommerceSubscriptionRepository subscriptions;
    private final SubscriptionSubjectRepository subjects;
    private final AuthEligibility eligibility;
    private final EntitlementSnapshots snapshotPublisher;
    private final PlanCatalogLookup plans;
    private final Clock clock;

    @Transactional
    public void accept(String provider, String account, byte[] rawBody, BillingProvider.NormalizedWebhook event) {
        String normalizedProvider = provider.toUpperCase(java.util.Locale.ROOT);
        if (inbox.exists(normalizedProvider, account, event.providerEventId())) return;
        BillingWebhookInbox record = BillingWebhookInbox.builder()
                .provider(normalizedProvider)
                .providerAccountKey(account)
                .providerEventId(event.providerEventId())
                .payloadHash(hash(rawBody))
                .providerObjectRef(event.objectReference())
                .providerObjectVersion(event.objectVersion())
                .receivedAt(now())
                .processingStatus("RECEIVED")
                .build();
        if (!inbox.insertIfAbsent(record)) {
            return;
        }
        subscriptions.findByProviderRef(normalizedProvider, account, event.objectReference()).ifPresent(subscription -> {
            long incomingVersion = event.objectVersion();
            if (incomingVersion > 0 && incomingVersion <= subscription.getProviderObjectVersion()) {
                record.setProcessingStatus("STALE");
                record.setProcessedAt(now());
                inbox.save(record);
                return;
            }
            if (incomingVersion > 0) {
                subscription.setProviderObjectVersion(incomingVersion);
            }
            String nextBillingStatus = normalizeBillingStatus(event.eventType(), event.status());
            if (!isLegalBillingTransition(subscription.getBillingStatus(), nextBillingStatus)) {
                record.setProcessingStatus("INVALID_TRANSITION");
                record.setFailureCode("SUBSCRIPTION_TRANSITION_INVALID");
                record.setProcessedAt(now());
                inbox.save(record);
                return;
            }
            subscription.advanceVersion();
            subscription.setBillingStatus(nextBillingStatus);
            String accessStatus = normalizeAccessStatus(event.eventType(), event.status());
            if (!eligible(subscription)) {
                // Billing confirmation never bypasses the latest committed
                // authorization projection. Keep the provider status for
                // reconciliation, but fail closed for local access.
                accessStatus = "INELIGIBLE";
            }
            subscription.setAccessStatus(accessStatus);
            subscription.setUpdatedAt(now());
            subscriptions.save(subscription);
            snapshotPublisher.publish(subscription.getSubjectId(), snapshot(subscription));
        });
        if (!"INVALID_TRANSITION".equals(record.getProcessingStatus())) {
            record.setProcessingStatus("PROCESSED");
            record.setProcessedAt(now());
        }
        inbox.save(record);
    }

    private boolean eligible(CommerceSubscription subscription) {
        return subjects.findById(subscription.getSubjectId())
                .map(subject -> eligibility.isActive(subject.getSubjectReference()))
                .orElse(false);
    }

    private boolean isLegalBillingTransition(String current, String next) {
        if (current == null || current.isBlank() || current.equalsIgnoreCase(next)) return true;
        try {
            SubscriptionLifecycle.requireTransition(
                    SubscriptionBillingStatus.valueOf(current.toUpperCase(java.util.Locale.ROOT)),
                    SubscriptionBillingStatus.valueOf(next.toUpperCase(java.util.Locale.ROOT)));
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    private Map<String, Object> snapshot(CommerceSubscription subscription) {
        Map<String, Object> payload = new LinkedHashMap<>();
        long version = Math.max(1L, subscription.getAggregateVersion());
        payload.put("subjectType", "USER");
        String userId = subjects.findById(subscription.getSubjectId())
                .map(SubscriptionSubject::getSubjectReference).orElse(subscription.getSubjectId());
        payload.put("subjectId", subscription.getSubjectId());
        payload.put("userId", userId);
        payload.put("entitlementVersion", version);
        payload.put("accessStatus", subscription.getAccessStatus());
        Instant periodStart = toInstant(subscription.getCurrentPeriodStart());
        Instant periodEnd = toInstant(subscription.getCurrentPeriodEnd());
        payload.put("effectiveFrom", periodStart == null ? nowInstant() : periodStart);
        payload.put("accessEffectiveUntil", periodEnd);
        payload.put("reconcileBy", periodEnd == null ? nowInstant().plusSeconds(300) : periodEnd);
        payload.put("currentPeriodStart", periodStart);
        payload.put("validUntil", periodEnd);
        payload.put("currentPeriodEnd", periodEnd);
        payload.put("sources", java.util.List.of(Map.of(
                "subscriptionId", subscription.getId(),
                "subscriptionAggregateVersion", version,
                "subscriptionSlot", "BASE",
                "productLine", "DEFAULT_USER_BASE",
                "planCode", subscription.getPlanVersionId(),
                "planVersion", 1L)));
        payload.put("features", plans.grantsOf(subscription.getPlanVersionId())
                .stream().map(value -> feature(value, periodStart, periodEnd)).toList());
        payload.put("occurredAt", nowInstant());
        return payload;
    }

    private Map<String, Object> feature(PlanFeatureGrant value, Instant periodStart, Instant periodEnd) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", value.featureKey());
        result.put("type", value.grantType());
        result.put("enabled", "BOOLEAN".equalsIgnoreCase(value.grantType())
                ? Boolean.TRUE.equals(value.booleanValue())
                : Boolean.TRUE.equals(value.quotaUnlimited()) || (value.quotaLimit() != null && value.quotaLimit() > 0));
        result.put("limit", value.quotaLimit());
        result.put("unlimited", value.quotaUnlimited());
        result.put("unit", value.quotaUnit());
        result.put("resetPolicy", value.resetPolicy());
        result.put("periodStart", periodStart);
        result.put("periodEnd", periodEnd);
        result.put("effectiveUntil", periodEnd);
        return result;
    }

    private String normalizeBillingStatus(String eventType, String providerStatus) {
        if (eventType != null && eventType.contains("invoice.payment_failed")) return "PAST_DUE";
        if (eventType != null && eventType.contains("deleted")) return "CANCELED";
        return providerStatus == null || providerStatus.isBlank() ? "ACTIVE" : providerStatus.toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeAccessStatus(String eventType, String providerStatus) {
        if (eventType != null && (eventType.contains("deleted") || eventType.contains("payment_failed"))) return "INELIGIBLE";
        return "ACTIVE";
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(nowInstant(), ZoneOffset.UTC);
    }

    private Instant nowInstant() {
        return Instant.now(clock);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private String hash(byte[] rawBody) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawBody == null ? new byte[0] : rawBody));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash billing webhook", exception);
        }
    }
}
