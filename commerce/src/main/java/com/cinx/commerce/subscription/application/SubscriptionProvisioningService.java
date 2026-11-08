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
import com.cinx.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Explicit repair/provisioning seam used by onboarding and auth reconciliation. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionProvisioningService {
    private static final String BASE = "BASE";
    private static final String FREE_PLAN = "LEARNER_FREE";

    private final SubscriptionSubjectRepository subjects;
    private final CurrentSubscriptionPointerRepository pointers;
    private final CommerceSubscriptionRepository subscriptions;
    private final PlanCatalogLookup plans;
    private final EntitlementSnapshots snapshotPublisher;
    private final ObjectProvider<BillingProvider> billingProvider;
    private final Clock clock;

    @Value("${cinx.subscription.billing-enabled:false}")
    private boolean billingEnabled;

    @Transactional
    public CommerceSubscription ensureFreeSubscription(String userId, String reason) {
        if (userId == null || userId.isBlank()) {
            throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID, "User identity is required");
        }
        SubscriptionSubject subject = subjects.lockOrCreate(SubscriptionSubject.builder().id(UUID.randomUUID().toString())
                .subjectType("USER").subjectReference(userId).createdAt(now()).build());
        CurrentSubscriptionPointer pointer = pointers.lock(subject.getId(), BASE).orElse(null);
        if (pointer != null) {
            return subscriptions.findById(pointer.getSubscriptionId()).orElseThrow(() ->
                    new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID, "Current subscription pointer is broken"));
        }
        PlanVersionRef version = freePlanVersion();
        CommerceSubscription subscription = createFreeSubscription(subject, version);
        pointers.save(CurrentSubscriptionPointer.builder().subjectId(subject.getId()).subscriptionSlotCode(BASE)
                .subscriptionId(subscription.getId()).activeCommandStartedAt(subscription.getCreatedAt()).optimisticVersion(0L).build());
        snapshotPublisher.publish(subject.getId(), snapshot(subscription, userId, version));
        return subscription;
    }

    /** Replace an ineligible current paid subscription through the data-managed free binding. */
    @Transactional
    public CommerceSubscription fallbackToFreeSubscription(String userId, String reason) {
        SubscriptionSubject subject = subjects.lockByTypeAndReference("USER", userId)
                .orElse(null);
        if (subject == null) return ensureFreeSubscription(userId, reason);
        CurrentSubscriptionPointer pointer = pointers.lock(subject.getId(), BASE)
                .orElse(null);
        if (pointer == null) return ensureFreeSubscription(userId, reason);
        CommerceSubscription current = subscriptions.findById(pointer.getSubscriptionId()).orElseThrow(() ->
                new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID, "Current subscription pointer is broken"));
        PlanVersionRef freeVersion = freePlanVersion();
        if (current.getPlanVersionId().equals(freeVersion.id())) {
            return current;
        }
        BillingProvider provider = billingProvider.getIfAvailable();
        if (billingEnabled && provider != null && current.getProviderSubscriptionRef() != null) {
            try {
                provider.cancelAtPeriodEnd(current.getProviderSubscriptionRef(),
                        "eligibility-loss-" + current.getId());
            } catch (RuntimeException exception) {
                // Access is still revoked immediately. Reconciliation will
                // retry the provider cancellation without reopening access.
                log.warn("Unable to schedule provider cancellation subscriptionId={}", current.getId(), exception);
            }
        }
        current.setAccessStatus("INELIGIBLE");
        current.setBillingStatus("INELIGIBLE");
        current.advanceVersion();
        current.setUpdatedAt(now());
        subscriptions.save(current);
        CommerceSubscription free = createFreeSubscription(subject, freeVersion);
        free.setAggregateVersion(Math.max(1L, current.getAggregateVersion() + 1));
        free.setUpdatedAt(now());
        subscriptions.save(free);
        pointer.setSubscriptionId(free.getId());
        pointer.setActiveCommandStartedAt(free.getCreatedAt());
        pointers.save(pointer);
        snapshotPublisher.publish(subject.getId(), snapshot(free, userId, freeVersion));
        return free;
    }

    private PlanVersionRef freePlanVersion() {
        return plans.findVersion(FREE_PLAN, 1).orElseThrow(() -> new ConflictException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                plans.planExists(FREE_PLAN) ? "LEARNER_FREE version is unavailable" : "LEARNER_FREE bootstrap is unavailable"));
    }

    private CommerceSubscription createFreeSubscription(SubscriptionSubject subject, PlanVersionRef version) {
        LocalDateTime start = now();
        LocalDateTime end = YearMonth.from(start).plusMonths(1).atDay(1).atStartOfDay();
        return subscriptions.save(CommerceSubscription.builder()
                .id(UUID.randomUUID().toString()).subjectId(subject.getId()).planVersionId(version.id())
                .origin("INTERNAL_FREE").billingStatus("ACTIVE").accessStatus("ACTIVE")
                .currentPeriodStart(start).currentPeriodEnd(end).cancelAtPeriodEnd(false)
                .aggregateVersion(1L).optimisticVersion(0L).createdAt(start).updatedAt(start).build());
    }

    @Transactional
    public void applyAuthorization(String userId, String status, List<String> roles) {
        boolean active = "ACTIVE".equalsIgnoreCase(status);
        boolean learner = roles != null && roles.stream().anyMatch(role -> "LEARNER".equalsIgnoreCase(role));
        if (active && learner) {
            ensureFreeSubscription(userId, "AUTHORIZATION_RECONCILIATION");
            return;
        }
        if (active) {
            fallbackToFreeSubscription(userId, "AUTHORIZATION_ELIGIBILITY_LOSS");
            return;
        }
        subjects.findByTypeAndReference("USER", userId).ifPresent(subject ->
                pointers.lock(subject.getId(), BASE).ifPresent(pointer ->
                        subscriptions.findById(pointer.getSubscriptionId()).ifPresent(subscription -> {
                            if (!"INELIGIBLE".equals(subscription.getAccessStatus())) {
                                subscription.setAccessStatus("INELIGIBLE");
                                subscription.advanceVersion();
                                subscription.setUpdatedAt(now());
                                subscriptions.save(subscription);
                                snapshotPublisher.publish(subject.getId(), snapshot(subscription, userId, null));
                            }
                        })));
    }

    private Map<String, Object> snapshot(CommerceSubscription subscription, String userId, PlanVersionRef planVersion) {
        Instant emittedAt = clock.instant();
        Instant start = toInstant(subscription.getCurrentPeriodStart());
        Instant end = toInstant(subscription.getCurrentPeriodEnd());
        long version = Math.max(1L, subscription.getAggregateVersion());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subjectType", "USER");
        result.put("subjectId", subscription.getSubjectId());
        result.put("userId", userId);
        result.put("entitlementVersion", version);
        result.put("accessStatus", subscription.getAccessStatus());
        result.put("effectiveFrom", start == null ? emittedAt : start);
        result.put("accessEffectiveUntil", end);
        result.put("reconcileBy", end == null ? emittedAt.plusSeconds(300) : end);
        result.put("currentPeriodStart", start);
        result.put("currentPeriodEnd", end);
        result.put("sources", List.of(Map.of(
                "subscriptionId", subscription.getId(),
                "subscriptionAggregateVersion", version,
                "subscriptionSlot", BASE,
                "productLine", FREE_PLAN,
                "planCode", planVersion == null ? subscription.getPlanVersionId() : planVersion.planCode(),
                "planVersion", planVersion == null ? 1L : planVersion.versionNumber())));
        result.put("features", planVersion == null ? List.of() : plans.grantsOf(planVersion.id()).stream()
                .map(value -> feature(value, start, end))
                .toList());
        result.put("occurredAt", emittedAt);
        return result;
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

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
