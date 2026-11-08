package com.cinx.commerce.subscription.application;

import com.cinx.commerce.catalog.application.PlanCatalogLookup;
import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.catalog.application.query.SaleableOffer;
import com.cinx.commerce.subscription.application.command.AdjustSubscriptionCommand;
import com.cinx.commerce.subscription.application.query.CheckoutSessionView;
import com.cinx.commerce.subscription.application.query.SubscriptionView;
import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CommerceSubscriptionRepository;
import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointerRepository;
import com.cinx.commerce.subscription.domain.SubscriptionAdjustment;
import com.cinx.commerce.subscription.domain.SubscriptionAdjustmentRepository;
import com.cinx.commerce.subscription.domain.SubscriptionCommand;
import com.cinx.commerce.subscription.domain.SubscriptionCommandRepository;
import com.cinx.commerce.subscription.domain.SubscriptionCommandType;
import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import com.cinx.commerce.subscription.domain.SubscriptionSubjectRepository;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionSubjectRepository subjects;
    private final CurrentSubscriptionPointerRepository pointers;
    private final SubscriptionCommandRepository commands;
    private final CommerceSubscriptionRepository subscriptions;
    private final PlanCatalogLookup plans;
    private final ObjectProvider<BillingProvider> billingProvider;
    private final Clock clock;
    private final SubscriptionAdjustmentRepository adjustments;
    private final ObjectMapper objectMapper;

    @Value("${cinx.subscription.billing-enabled:false}")
    private boolean billingEnabled;

    @Transactional(readOnly = true)
    public SubscriptionView read(String userId) {
        SubscriptionSubject subject = subjects.findByTypeAndReference("USER", userId).orElse(null);
        if (subject == null) return null;
        CommerceSubscription current = pointers.find(subject.getId(), "BASE")
                .flatMap(pointer -> subscriptions.findById(pointer.getSubscriptionId()))
                .orElseGet(() -> subscriptions.findAllBySubjectId(subject.getId()).stream()
                        .findFirst().orElse(null));
        if (current == null) return null;
        String planCode = plans.planCodeOfVersion(current.getPlanVersionId()).orElse(current.getPlanVersionId());
        return new SubscriptionView(current.getId(), planCode, current.getBillingStatus(),
                current.getAccessStatus(), current.getCurrentPeriodEnd(), current.isCancelAtPeriodEnd(), current.getAggregateVersion());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> entitlementSnapshot(String userId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Instant emittedAt = nowInstant();
        snapshot.put("subjectType", "USER");
        SubscriptionSubject subject = subjects.findByTypeAndReference("USER", userId).orElse(null);
        snapshot.put("subjectId", subject == null ? userId : subject.getId());
        snapshot.put("userId", userId);
        snapshot.put("entitlementVersion", 0L);
        snapshot.put("accessStatus", "INACTIVE");
        snapshot.put("effectiveFrom", emittedAt);
        snapshot.put("accessEffectiveUntil", null);
        snapshot.put("reconcileBy", emittedAt.plusSeconds(300));
        snapshot.put("currentPeriodStart", null);
        snapshot.put("currentPeriodEnd", null);
        snapshot.put("sources", List.of());
        snapshot.put("features", List.of());
        SubscriptionView current = read(userId);
        if (current != null) {
            long version = Math.max(1L, current.entitlementVersion());
            LocalDateTime currentPeriodStart = subject == null ? null : subscriptions
                    .findAllBySubjectId(subject.getId()).stream()
                    .filter(value -> value.getId().equals(current.subscriptionId()))
                    .findFirst()
                    .map(CommerceSubscription::getCurrentPeriodStart)
                    .orElse(null);
            Instant periodStart = toInstant(currentPeriodStart);
            Instant periodEnd = toInstant(current.currentPeriodEnd());
            snapshot.put("entitlementVersion", version);
            snapshot.put("accessStatus", current.accessStatus());
            snapshot.put("subscriptionId", current.subscriptionId());
            snapshot.put("currentPeriodEnd", periodEnd);
            snapshot.put("accessEffectiveUntil", periodEnd);
            snapshot.put("currentPeriodStart", periodStart);
            snapshot.put("effectiveFrom", periodStart == null ? emittedAt : periodStart);
            snapshot.put("reconcileBy", periodEnd == null ? emittedAt.plusSeconds(300) : periodEnd);
            snapshot.put("sources", List.of(Map.of(
                    "subscriptionId", current.subscriptionId(),
                    "subscriptionAggregateVersion", version,
                    "subscriptionSlot", "BASE",
                    "productLine", "DEFAULT_USER_BASE",
                    "planCode", current.planCode(),
                    "planVersion", 1L)));
            String planVersionId = currentEntityId(subject, current.subscriptionId());
            snapshot.put("features", plans.grantsOf(planVersionId).stream()
                    .map(value -> feature(value, periodStart, periodEnd))
                    .toList());
        }
        snapshot.put("occurredAt", emittedAt);
        return snapshot;
    }

    @Transactional
    public CheckoutSessionView createCheckout(String userId, String idempotencyKey, String offerId, String returnPath) {
        requireKey(idempotencyKey);
        SaleableOffer offer = plans.resolveSaleableOffer(offerId, now());
        String normalizedPath = validateReturnPath(returnPath);
        String fingerprint = IdempotencyFingerprint.sha256(userId, SubscriptionCommandType.CREATE_CHECKOUT.name(),
                "offer=" + offer.offerId() + "&returnPath=" + normalizedPath);
        SubscriptionSubject subject = subject(userId);
        SubscriptionCommand command = reserve(subject, idempotencyKey, fingerprint, SubscriptionCommandType.CREATE_CHECKOUT);
        if (command.getResponseReference() != null) {
            return new CheckoutSessionView(command.getCommandId(), command.getResponseReference(), null, command.getStatus());
        }
        BillingProvider provider = billingProvider.getIfAvailable();
        if (offer.priceId() == null) {
            command.setStatus("COMPLETED");
            commands.save(command);
            return new CheckoutSessionView(command.getCommandId(), null, null, command.getStatus());
        }
        if (!billingEnabled) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                    "Subscription billing is disabled");
        }
        if (provider == null) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Billing provider is not enabled");
        }
        BillingProvider.CheckoutSession session = provider.createSubscriptionCheckout(
                new BillingProvider.CheckoutCommand(command.getCommandId(), userId, offer.priceId(), normalizedPath, normalizedPath));
        command.setResponseReference(session.checkoutUrl());
        command.setStatus("PROVIDER_PENDING");
        commands.save(command);
        return new CheckoutSessionView(command.getCommandId(), session.checkoutUrl(), session.expiresAt(), command.getStatus());
    }

    @Transactional
    public void cancel(String userId, String idempotencyKey) {
        requireKey(idempotencyKey);
        SubscriptionSubject subject = subject(userId);
        SubscriptionView current = read(userId);
        if (current == null) throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID, "No current subscription");
        if (!isCancelable(current.billingStatus())) {
            throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID,
                    "Subscription cannot be canceled from its current state");
        }
        SubscriptionCommand command = reserve(subject, idempotencyKey,
                IdempotencyFingerprint.sha256(userId, "CANCEL", current.subscriptionId()), SubscriptionCommandType.CANCEL);
        if ("COMPLETED".equals(command.getStatus())) return;
        subscriptions.findById(current.subscriptionId()).ifPresent(value -> {
            BillingProvider provider = billingProvider.getIfAvailable();
            if (billingEnabled && provider != null && value.getProviderSubscriptionRef() != null) {
                BillingProvider.ProviderSubscription remote = provider.cancelAtPeriodEnd(
                        value.getProviderSubscriptionRef(), command.getCommandId());
                value.setCurrentPeriodEnd(remote.periodEnd() == null ? value.getCurrentPeriodEnd()
                        : LocalDateTime.ofInstant(remote.periodEnd(), ZoneOffset.UTC));
                value.setProviderObjectVersion(Math.max(value.getProviderObjectVersion(), remote.providerVersion()));
            }
            value.setCancelAtPeriodEnd(true);
            value.setUpdatedAt(now());
            subscriptions.save(value);
        });
        command.setStatus("COMPLETED");
        commands.save(command);
    }

    @Transactional
    public SubscriptionView change(String userId, String idempotencyKey, String offerId) {
        requireKey(idempotencyKey);
        SaleableOffer offer = plans.resolveSaleableOffer(offerId, now());
        SubscriptionSubject subject = subject(userId);
        SubscriptionView current = read(userId);
        if (current == null) throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID, "No current subscription");
        if (!isChangeable(current.billingStatus())) {
            throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID,
                    "Subscription cannot be changed from its current state");
        }
        String fingerprint = IdempotencyFingerprint.sha256(userId, SubscriptionCommandType.CHANGE.name(),
                "current=" + current.subscriptionId() + "&offer=" + offer.offerId());
        SubscriptionCommand command = reserve(subject, idempotencyKey, fingerprint, SubscriptionCommandType.CHANGE);
        if ("COMPLETED".equals(command.getStatus())) return current;
        if (!billingEnabled || offer.priceId() == null) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                    "Subscription changes require an enabled billing provider");
        }
        CommerceSubscription currentSubscription = subscriptions.findById(current.subscriptionId())
                .orElseThrow(() -> new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID,
                        "Current subscription is unavailable"));
        BillingProvider provider = billingProvider.getIfAvailable();
        if (provider == null || currentSubscription.getProviderSubscriptionRef() == null) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                    "Subscription provider state is unavailable");
        }
        BillingProvider.ProviderSubscription remote = provider.changeSubscription(new BillingProvider.ChangeCommand(
                command.getCommandId(), currentSubscription.getProviderSubscriptionRef(), offer.priceId()));
        currentSubscription.setProviderObjectVersion(Math.max(currentSubscription.getProviderObjectVersion(),
                remote.providerVersion()));
        currentSubscription.setUpdatedAt(now());
        subscriptions.save(currentSubscription);
        // Access is intentionally unchanged until the confirmation webhook.
        command.setStatus("PROVIDER_PENDING");
        commands.save(command);
        return current;
    }

    @Transactional
    public void resume(String userId, String idempotencyKey) {
        requireKey(idempotencyKey);
        SubscriptionSubject subject = subject(userId);
        SubscriptionView current = read(userId);
        if (current == null) throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID, "No current subscription");
        if (!isCancelable(current.billingStatus())) {
            throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID,
                    "Subscription cannot be resumed from its current state");
        }
        SubscriptionCommand command = reserve(subject, idempotencyKey,
                IdempotencyFingerprint.sha256(userId, "RESUME", current.subscriptionId()), SubscriptionCommandType.RESUME);
        if ("COMPLETED".equals(command.getStatus())) return;
        subscriptions.findById(current.subscriptionId()).ifPresent(value -> {
            BillingProvider provider = billingProvider.getIfAvailable();
            if (billingEnabled && provider != null && value.getProviderSubscriptionRef() != null) {
                BillingProvider.ProviderSubscription remote = provider.resume(
                        value.getProviderSubscriptionRef(), command.getCommandId());
                value.setProviderObjectVersion(Math.max(value.getProviderObjectVersion(), remote.providerVersion()));
            }
            value.setCancelAtPeriodEnd(false);
            value.setUpdatedAt(now());
            subscriptions.save(value);
        });
        command.setStatus("COMPLETED");
        commands.save(command);
    }

    @Transactional
    public SubscriptionView adjust(String subscriptionId, AdjustSubscriptionCommand request, String actor) {
        return adjust(subscriptionId, "legacy-adjust-" + UUID.randomUUID(), request, actor);
    }

    @Transactional
    public SubscriptionView adjust(String subscriptionId, String idempotencyKey,
                                   AdjustSubscriptionCommand request, String actor) {
        requireKey(idempotencyKey);
        CommerceSubscription subscription = subscriptions.findById(subscriptionId)
                .orElseThrow(() -> new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID, "Subscription not found"));
        SubscriptionSubject subject = subjects.findById(subscription.getSubjectId())
                .orElseThrow(() -> new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID,
                        "Subscription subject is unavailable"));
        String fingerprint = IdempotencyFingerprint.sha256(subject.getSubjectReference(),
                SubscriptionCommandType.ADJUST.name(), subscriptionId + "|" + requestFingerprint(request));
        SubscriptionCommand command = reserve(subject, idempotencyKey, fingerprint, SubscriptionCommandType.ADJUST);
        if ("COMPLETED".equals(command.getStatus())) {
            return toView(subscription);
        }
        String valueJson;
        try {
            valueJson = objectMapper.writeValueAsString(request.value() == null ? Map.of() : request.value());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Adjustment value is not serializable", exception);
        }
        adjustments.save(SubscriptionAdjustment.builder()
                .id(UUID.randomUUID().toString())
                .subscriptionId(subscriptionId)
                .featureKey(request.featureKey())
                .valueJson(valueJson)
                .reason(request.reason())
                .effectiveFrom(request.effectiveFrom())
                .expiresAt(request.expiresAt())
                .createdBy(actor == null ? "staff" : actor)
                .createdAt(now())
                .build());
        subscription.setAggregateVersion(subscription.getAggregateVersion() + 1);
        subscription.setUpdatedAt(now());
        subscriptions.save(subscription);
        command.setStatus("COMPLETED");
        commands.save(command);
        return toView(subscription);
    }

    private String requestFingerprint(AdjustSubscriptionCommand request) {
        return String.join("|", request.featureKey() == null ? "" : request.featureKey(),
                request.value() == null ? "{}" : request.value().toString(), request.reason(),
                String.valueOf(request.effectiveFrom()), String.valueOf(request.expiresAt()));
    }

    private SubscriptionView toView(CommerceSubscription subscription) {
        return new SubscriptionView(subscription.getId(), subscription.getPlanVersionId(),
                subscription.getBillingStatus(), subscription.getAccessStatus(), subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(), subscription.getAggregateVersion());
    }

    private SubscriptionSubject subject(String userId) {
        if (userId == null || userId.isBlank()) throw new ForbiddenException("Authenticated user is required");
        return subjects.findOrCreate(SubscriptionSubject.builder().id(UUID.randomUUID().toString())
                .subjectType("USER").subjectReference(userId).createdAt(now()).build());
    }

    private SubscriptionCommand reserve(SubscriptionSubject subject, String key, String fingerprint,
                                        SubscriptionCommandType type) {
        Optional<SubscriptionCommand> existing = commands.findBySubjectIdAndIdempotencyKey(subject.getId(), key);
        if (existing.isPresent()) {
            return sameRequest(existing.get(), fingerprint);
        }
        String commandId = UUID.randomUUID().toString();
        SubscriptionCommand candidate = SubscriptionCommand.builder()
                .commandId(commandId).subjectId(subject.getId()).subscriptionSlotCode("BASE")
                .idempotencyKey(key).requestFingerprint(fingerprint).commandType(type.name())
                .providerIdempotencyKey("cinx_" + commandId).status("PENDING")
                .createdAt(now()).build();
        // another request with the same key may have won the race: then its command is the answer
        return commands.insertIfAbsent(candidate).orElseGet(() -> commands.findBySubjectIdAndIdempotencyKey(subject.getId(), key)
                .map(raced -> sameRequest(raced, fingerprint))
                .orElseThrow(() -> new IllegalStateException("Subscription command could not be recorded")));
    }

    private SubscriptionCommand sameRequest(SubscriptionCommand existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new ConflictException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency-Key was already used with another request");
        }
        return existing;
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 255) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A valid Idempotency-Key header is required");
        }
    }

    private boolean isCancelable(String status) {
        return "PENDING".equalsIgnoreCase(status) || "TRIALING".equalsIgnoreCase(status)
                || "ACTIVE".equalsIgnoreCase(status) || "PAST_DUE".equalsIgnoreCase(status)
                || "GRACE".equalsIgnoreCase(status);
    }

    private boolean isChangeable(String status) {
        return "TRIALING".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)
                || "PAST_DUE".equalsIgnoreCase(status) || "GRACE".equalsIgnoreCase(status);
    }

    private String validateReturnPath(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("\\") || path.contains("\n")) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Return path is not allowed");
        }
        return path;
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

    private String currentEntityId(SubscriptionSubject subject, String subscriptionId) {
        if (subject == null) return subscriptionId;
        return subscriptions.findAllBySubjectId(subject.getId()).stream()
                .filter(value -> value.getId().equals(subscriptionId))
                .map(CommerceSubscription::getPlanVersionId)
                .findFirst()
                .orElse(subscriptionId);
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
}
