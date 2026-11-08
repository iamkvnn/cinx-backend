package com.cinx.commerce.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port: where the subscription plans are stored: the plans and their product lines, their numbered versions with the
 * offers, prices, entitlements and translations of each, the billing policies and the feature registry.
 */
public interface PlanCatalogRepository {
    Optional<SubscriptionPlan> findPlanByCode(String code);

    Optional<SubscriptionPlan> findPlanById(String id);

    SubscriptionPlan savePlan(SubscriptionPlan plan);

    Optional<PlanProductLine> findProductLineByCode(String code);

    Optional<PlanProductLine> findProductLineById(String id);

    PlanProductLine saveProductLine(PlanProductLine line);

    Optional<BillingPolicyVersion> findBillingPolicy(String code, long versionNumber);

    BillingPolicyVersion saveBillingPolicy(BillingPolicyVersion policy);

    /** The published versions, grouped by plan, the newest first within a plan. */
    List<SubscriptionPlanVersion> findPublishedVersions();

    /** Every version of the plan, the newest first. */
    List<SubscriptionPlanVersion> findVersionsOfPlan(String planId);

    Optional<SubscriptionPlanVersion> findVersion(String planId, long versionNumber);

    Optional<SubscriptionPlanVersion> findVersionById(String id);

    SubscriptionPlanVersion saveVersion(SubscriptionPlanVersion version);

    List<PlanOffer> findOffers(String planVersionId, String status);

    Optional<PlanOffer> findOfferById(String id);

    Optional<PlanPrice> findPrice(String id, String planId);

    /** The version's entitlements in feature-key order. */
    List<PlanEntitlement> findEntitlements(String planVersionId);

    Optional<CommerceFeatureRegistry> findFeature(String featureKey);

    List<CommerceFeatureRegistry> findFeatures(Collection<String> featureKeys);

    Optional<PlanVersionTranslation> findTranslation(String planVersionId, String locale);
}
