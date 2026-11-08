package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.command.CreatePlanDraftCommand;
import com.cinx.commerce.catalog.application.query.PublishedPlanView;
import com.cinx.commerce.catalog.domain.BillingPolicyVersion;
import com.cinx.commerce.catalog.domain.CommerceFeatureRegistry;
import com.cinx.commerce.catalog.domain.PlanCatalogRepository;
import com.cinx.commerce.catalog.domain.PlanEntitlement;
import com.cinx.commerce.catalog.domain.PlanOffer;
import com.cinx.commerce.catalog.domain.PlanPrice;
import com.cinx.commerce.catalog.domain.PlanProductLine;
import com.cinx.commerce.catalog.domain.PlanVersionTranslation;
import com.cinx.commerce.catalog.domain.SubscriptionPlan;
import com.cinx.commerce.catalog.domain.SubscriptionPlanVersion;
import com.cinx.commerce.subscription.application.BillingAvailability;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanCatalogService {
    private final PlanCatalogRepository catalog;
    private final BillingAvailability billing;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PublishedPlanView> publishedPlans(String market, String locale) {
        return catalog.findPublishedVersions().stream()
                // A plan may retain historical published versions; public
                // catalog reads expose only the newest immutable version.
                .collect(Collectors.toMap(SubscriptionPlanVersion::getPlanId,
                        value -> value, (first, ignored) -> first,
                        LinkedHashMap::new))
                .values().stream()
                .map(version -> toView(version, market, locale))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublishedPlanView publishedPlan(String code, String market, String locale) {
        SubscriptionPlan plan = catalog.findPlanByCode(code)
                .orElseThrow(() -> new NotFoundException("Published plan not found"));
        SubscriptionPlanVersion version = catalog.findVersion(plan.getId(), latestPublishedVersion(plan.getId()))
                .orElseThrow(() -> new NotFoundException("Published plan version not found"));
        return toView(version, market, locale);
    }

    @Transactional
    public PublishedPlanView createDraft(CreatePlanDraftCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        PlanProductLine line = catalog.findProductLineByCode(command.productLine()).orElseGet(() -> catalog.saveProductLine(
                PlanProductLine.builder().id(UUID.randomUUID().toString()).code(command.productLine())
                        .customerSegment(command.productLine()).eligibilityPolicyKey("REVIEW_REQUIRED")
                        .subscriptionSlotCode("BASE").displayOrder(0).status("ACTIVE")
                        .createdAt(now).updatedAt(now).build()));
        SubscriptionPlan plan = catalog.findPlanByCode(command.code()).orElseGet(() -> catalog.savePlan(
                SubscriptionPlan.builder().id(UUID.randomUUID().toString()).productLineId(line.getId())
                        .code(command.code()).lifecycleStatus("DRAFT").createdAt(now).updatedAt(now).build()));
        if (!line.getId().equals(plan.getProductLineId())) {
            throw new BadRequestException(ErrorCode.PLAN_NOT_ELIGIBLE,
                    "Plan code is already owned by another product line");
        }
        BillingPolicyVersion policy = catalog.findBillingPolicy("STANDARD_NO_TRIAL", 1)
                .orElseGet(() -> catalog.saveBillingPolicy(BillingPolicyVersion.builder().id(UUID.randomUUID().toString())
                        .code("STANDARD_NO_TRIAL").versionNumber(1).publicationStatus("DRAFT")
                        .trialEnabled(false).renewalFailurePolicy("ENTER_GRACE").graceDurationSeconds(72 * 3600L)
                        .accessDuringGrace(true).quotaDuringGracePolicy("DENY_METERED")
                        .quotaRolloverEnabled(false).quotaOverageEnabled(false)
                        .upgradeEffectivePolicy("PROVIDER_PAYMENT_CONFIRMED").upgradeProrationPolicy("ALWAYS_INVOICE")
                        .upgradePendingUpdateRequired(true).downgradeEffectivePolicy("CURRENT_PERIOD_END")
                        .cancellationEffectivePolicy("CURRENT_PERIOD_END").eligibilityLossAccessPolicy("IMMEDIATE_INELIGIBLE")
                        .eligibilityLossProviderPolicy("CANCEL_AT_PERIOD_END").eligibilityLossRefundPolicy("MANUAL_REVIEW")
                        .build()));
        long nextVersion = catalog.findVersionsOfPlan(plan.getId()).stream()
                .mapToLong(SubscriptionPlanVersion::getVersionNumber).max().orElse(0L) + 1;
        SubscriptionPlanVersion version = catalog.saveVersion(SubscriptionPlanVersion.builder()
                .id(UUID.randomUUID().toString()).planId(plan.getId()).billingPolicyVersionId(policy.getId())
                .versionNumber(nextVersion).publicationStatus("DRAFT")
                .defaultLocale(command.defaultLocale() == null ? "vi-VN" : command.defaultLocale())
                .build());
        return toView(version, "VN", null);
    }

    @Transactional
    public PublishedPlanView publish(String code, long versionNumber, String actor) {
        SubscriptionPlan plan = catalog.findPlanByCode(code)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        SubscriptionPlanVersion version = catalog.findVersion(plan.getId(), versionNumber)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));
        if (!"DRAFT".equals(version.getPublicationStatus())) {
            throw new ConflictException(ErrorCode.SUBSCRIPTION_TRANSITION_INVALID,
                    "Only a draft plan version can be published");
        }
        validateSaleableOffers(plan, version);
        version.setPublicationStatus("PUBLISHED");
        version.setPublishedAt(LocalDateTime.now(clock));
        version.setPublishedBy(actor);
        plan.setLifecycleStatus("ACTIVE");
        plan.setUpdatedAt(LocalDateTime.now(clock));
        catalog.savePlan(plan);
        return toView(catalog.saveVersion(version), "VN", null);
    }

    @Transactional(readOnly = true)
    public PublishedPlanView validate(String code, long versionNumber) {
        SubscriptionPlan plan = catalog.findPlanByCode(code)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        SubscriptionPlanVersion version = catalog.findVersion(plan.getId(), versionNumber)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));
        if (version.getBillingPolicyVersionId() == null || version.getDefaultLocale() == null
                || version.getDefaultLocale().isBlank()) {
            throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                    "Plan version is missing a billing policy or locale");
        }
        validateSaleableOffers(plan, version);
        return toView(version, "VN", null);
    }

    @Transactional
    public void retire(String code, long versionNumber, String actor) {
        SubscriptionPlan plan = catalog.findPlanByCode(code)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        SubscriptionPlanVersion version = catalog.findVersion(plan.getId(), versionNumber)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));
        if ("RETIRED".equals(version.getPublicationStatus())) return;
        version.setPublicationStatus("RETIRED");
        catalog.saveVersion(version);
        if ("ACTIVE".equals(plan.getLifecycleStatus())) {
            plan.setLifecycleStatus("RETIRED");
            plan.setUpdatedAt(LocalDateTime.now(clock));
            catalog.savePlan(plan);
        }
    }

    private long latestPublishedVersion(String planId) {
        return catalog.findPublishedVersions().stream()
                .filter(version -> version.getPlanId().equals(planId))
                .mapToLong(SubscriptionPlanVersion::getVersionNumber).max()
                .orElseThrow(() -> new NotFoundException("Published plan version not found"));
    }

    private PublishedPlanView toView(SubscriptionPlanVersion version, String market, String locale) {
        SubscriptionPlan plan = catalog.findPlanById(version.getPlanId())
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        PlanProductLine line = catalog.findProductLineById(plan.getProductLineId())
                .orElseThrow(() -> new NotFoundException("Product line not found"));
        List<PublishedPlanView.Offer> planOffers = catalog.findOffers(version.getId(), "ACTIVE").stream()
                .filter(offer -> market == null || market.equalsIgnoreCase(offer.getMarketCode()))
                .map(offer -> toOfferView(offer, plan.getId()))
                .toList();
        PlanVersionTranslation text = translation(version, locale);
        return new PublishedPlanView(plan.getCode(), line.getCode(), version.getVersionNumber(),
                version.getDefaultLocale(),
                text == null ? plan.getCode() : text.getDisplayName(),
                text == null ? null : text.getDescription(),
                text == null ? null : text.getBadge(),
                billing.available(),
                saleableFeatures(version),
                planOffers);
    }

    private PublishedPlanView.Offer toOfferView(PlanOffer offer, String planId) {
        PlanPrice price = offer.getPriceId() == null ? null : catalog.findPrice(offer.getPriceId(), planId).orElse(null);
        return new PublishedPlanView.Offer(offer.getId(), offer.getMarketCode(), offer.getPriceId(), offer.getStatus(),
                price == null ? null : price.getAmount(),
                price == null ? null : price.getCurrency(),
                price == null ? null : price.getBillingInterval(),
                price == null ? null : price.getIntervalCount());
    }

    /** The requested locale, else the version's default one, else nothing (the caller falls back to the plan code). */
    private PlanVersionTranslation translation(SubscriptionPlanVersion version, String locale) {
        if (locale != null && !locale.isBlank()) {
            Optional<PlanVersionTranslation> requested = catalog.findTranslation(version.getId(), locale);
            if (requested.isPresent()) {
                return requested.get();
            }
        }
        return catalog.findTranslation(version.getId(), version.getDefaultLocale()).orElse(null);
    }

    /** Only features the registry marks saleable and active are shown to the public. */
    private List<PublishedPlanView.Feature> saleableFeatures(SubscriptionPlanVersion version) {
        List<PlanEntitlement> granted = catalog.findEntitlements(version.getId());
        Map<String, CommerceFeatureRegistry> registry = catalog
                .findFeatures(granted.stream().map(PlanEntitlement::getFeatureKey).toList()).stream()
                .collect(Collectors.toMap(CommerceFeatureRegistry::getFeatureKey, value -> value));
        return granted.stream()
                .filter(entitlement -> {
                    CommerceFeatureRegistry feature = registry.get(entitlement.getFeatureKey());
                    return feature != null && feature.isSaleable() && feature.isActive();
                })
                .map(entitlement -> {
                    boolean quota = "QUOTA".equals(registry.get(entitlement.getFeatureKey()).getValueType());
                    boolean unlimited = Boolean.TRUE.equals(entitlement.getQuotaUnlimited());
                    boolean enabled = quota
                            ? unlimited || (entitlement.getQuotaLimit() != null && entitlement.getQuotaLimit() > 0)
                            : Boolean.TRUE.equals(entitlement.getBooleanValue());
                    return new PublishedPlanView.Feature(entitlement.getFeatureKey(), registry.get(entitlement.getFeatureKey()).getValueType(),
                            enabled, entitlement.getQuotaLimit(), unlimited, entitlement.getQuotaUnit(), entitlement.getResetPolicy());
                })
                .toList();
    }

    private void validateSaleableOffers(SubscriptionPlan plan, SubscriptionPlanVersion version) {
        List<PlanOffer> activeOffers = catalog.findOffers(version.getId(), "ACTIVE");
        if (activeOffers.isEmpty()) {
            throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                    "A published version requires an active offer");
        }
        for (PlanEntitlement entitlement : catalog.findEntitlements(version.getId())) {
            CommerceFeatureRegistry definition = catalog.findFeature(entitlement.getFeatureKey())
                    .orElseThrow(() -> new BadRequestException(ErrorCode.FEATURE_NOT_ENTITLED,
                            "Plan references an unknown feature: " + entitlement.getFeatureKey()));
            boolean booleanGrant = "BOOLEAN".equalsIgnoreCase(entitlement.getGrantType());
            boolean quotaGrant = "QUOTA".equalsIgnoreCase(entitlement.getGrantType());
            if (!booleanGrant && !quotaGrant) {
                throw new BadRequestException(ErrorCode.FEATURE_NOT_ENTITLED,
                        "Unknown plan feature grant type: " + entitlement.getGrantType());
            }
            if (booleanGrant && (entitlement.getBooleanValue() == null
                    || entitlement.getQuotaLimit() != null
                    || Boolean.TRUE.equals(entitlement.getQuotaUnlimited())
                    || entitlement.getQuotaUnit() != null || entitlement.getResetPolicy() != null)) {
                throw new BadRequestException(ErrorCode.FEATURE_NOT_ENTITLED,
                        "BOOLEAN feature grants cannot contain quota fields");
            }
            if (quotaGrant && (entitlement.getQuotaUnit() == null || entitlement.getResetPolicy() == null
                    || (Boolean.TRUE.equals(entitlement.getQuotaUnlimited()) && entitlement.getQuotaLimit() != null)
                    || (!Boolean.TRUE.equals(entitlement.getQuotaUnlimited())
                    && (entitlement.getQuotaLimit() == null || entitlement.getQuotaLimit() < 0)))) {
                throw new BadRequestException(ErrorCode.FEATURE_NOT_ENTITLED,
                        "QUOTA feature grants require a unit, reset policy and one limit mode");
            }
            if (definition.getValueType() == null
                    || !definition.getValueType().equalsIgnoreCase(entitlement.getGrantType())
                    || (quotaGrant && (definition.getUnit() == null
                    || !definition.getUnit().equalsIgnoreCase(entitlement.getQuotaUnit())
                    || !containsResetPolicy(definition.getResetPolicies(), entitlement.getResetPolicy())))) {
                throw new BadRequestException(ErrorCode.FEATURE_NOT_ENTITLED,
                        "Plan feature grant metadata does not match the registry");
            }
            boolean granted = booleanGrant
                    ? Boolean.TRUE.equals(entitlement.getBooleanValue())
                    : Boolean.TRUE.equals(entitlement.getQuotaUnlimited())
                    || (entitlement.getQuotaLimit() != null && entitlement.getQuotaLimit() > 0);
            if (granted && (!definition.isActive() || !definition.isSaleable())) {
                throw new BadRequestException(ErrorCode.FEATURE_NOT_ENTITLED,
                        "Plan references a disabled or non-saleable feature: " + entitlement.getFeatureKey());
            }
        }
        for (PlanOffer offer : activeOffers) {
            if (offer.getSalesStart() != null && offer.getSalesEnd() != null
                    && !offer.getSalesStart().isBefore(offer.getSalesEnd())) {
                throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                        "Offer sales_start must be before sales_end");
            }
            if (!plan.getId().equals(offer.getPlanId()) || !version.getPlanId().equals(plan.getId())) {
                throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                        "Offer does not belong to the plan version");
            }
            if (offer.getPriceId() == null) continue; // free offers need no provider publication
            PlanPrice price = catalog.findPrice(offer.getPriceId(), plan.getId())
                    .orElseThrow(() -> new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                            "Offer price does not belong to the plan"));
            if (offer.getMarketCode() != null && price.getMarketCode() != null
                    && !offer.getMarketCode().equalsIgnoreCase(price.getMarketCode())) {
                throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                        "Offer market does not match its price");
            }
            if (price.getAmount() == null || price.getAmount().signum() < 0) {
                throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                        "Price amount must be non-negative");
            }
            try {
                CurrencyAmountValidator.toMinorAmount(price.getAmount(), price.getCurrency());
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, exception.getMessage());
            }
            if (!"RECONCILED".equalsIgnoreCase(price.getStatus())
                    || price.getProviderPriceRef() == null || price.getProviderPriceRef().isBlank()) {
                throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                        "Paid offer provider reference is not reconciled");
            }
        }
    }

    private boolean containsResetPolicy(String registryPolicies, String requested) {
        if (registryPolicies == null || requested == null) return false;
        return java.util.Arrays.stream(registryPolicies.split(","))
                .map(String::trim)
                .anyMatch(policy -> policy.equalsIgnoreCase(requested));
    }
}
