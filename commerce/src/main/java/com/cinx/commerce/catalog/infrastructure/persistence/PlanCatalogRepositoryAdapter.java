package com.cinx.commerce.catalog.infrastructure.persistence;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlanCatalogRepositoryAdapter implements PlanCatalogRepository {
    private final SubscriptionPlanJpaRepository plans;
    private final SubscriptionPlanVersionJpaRepository versions;
    private final PlanOfferJpaRepository offers;
    private final PlanPriceJpaRepository prices;
    private final PlanProductLineJpaRepository productLines;
    private final BillingPolicyVersionJpaRepository billingPolicies;
    private final PlanEntitlementJpaRepository entitlements;
    private final CommerceFeatureRegistryJpaRepository features;
    private final PlanVersionTranslationJpaRepository translations;
    private final SubscriptionPlanPersistenceMapper planMapper;
    private final SubscriptionPlanVersionPersistenceMapper versionMapper;
    private final PlanOfferPersistenceMapper offerMapper;
    private final PlanPricePersistenceMapper priceMapper;
    private final PlanProductLinePersistenceMapper lineMapper;
    private final BillingPolicyVersionPersistenceMapper policyMapper;
    private final PlanEntitlementPersistenceMapper entitlementMapper;
    private final CommerceFeatureRegistryPersistenceMapper featureMapper;
    private final PlanVersionTranslationPersistenceMapper translationMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionPlan> findPlanByCode(String code) {
        return plans.findByCode(code).map(planMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionPlan> findPlanById(String id) {
        return plans.findById(id).map(planMapper::toDomain);
    }

    @Override
    @Transactional
    public SubscriptionPlan savePlan(SubscriptionPlan plan) {
        SubscriptionPlanJpaEntity stored = plans.findById(plan.getId()).orElse(null);
        if (stored == null) {
            return planMapper.toDomain(plans.save(planMapper.toEntity(plan)));
        }
        planMapper.update(stored, plan);
        return planMapper.toDomain(plans.save(stored));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanProductLine> findProductLineByCode(String code) {
        return productLines.findByCode(code).map(lineMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanProductLine> findProductLineById(String id) {
        return productLines.findById(id).map(lineMapper::toDomain);
    }

    @Override
    @Transactional
    public PlanProductLine saveProductLine(PlanProductLine line) {
        PlanProductLineJpaEntity stored = productLines.findById(line.getId()).orElse(null);
        if (stored == null) {
            return lineMapper.toDomain(productLines.save(lineMapper.toEntity(line)));
        }
        lineMapper.update(stored, line);
        return lineMapper.toDomain(productLines.save(stored));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BillingPolicyVersion> findBillingPolicy(String code, long versionNumber) {
        return billingPolicies.findByCodeAndVersionNumber(code, versionNumber).map(policyMapper::toDomain);
    }

    @Override
    @Transactional
    public BillingPolicyVersion saveBillingPolicy(BillingPolicyVersion policy) {
        BillingPolicyVersionJpaEntity stored = billingPolicies.findById(policy.getId()).orElse(null);
        if (stored == null) {
            return policyMapper.toDomain(billingPolicies.save(policyMapper.toEntity(policy)));
        }
        policyMapper.update(stored, policy);
        return policyMapper.toDomain(billingPolicies.save(stored));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanVersion> findPublishedVersions() {
        return versions.findAllByPublicationStatusOrderByPlanIdAscVersionNumberDesc("PUBLISHED").stream()
                .map(versionMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanVersion> findVersionsOfPlan(String planId) {
        return versions.findAllByPlanIdOrderByVersionNumberDesc(planId).stream().map(versionMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionPlanVersion> findVersion(String planId, long versionNumber) {
        return versions.findByPlanIdAndVersionNumber(planId, versionNumber).map(versionMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionPlanVersion> findVersionById(String id) {
        return versions.findById(id).map(versionMapper::toDomain);
    }

    /** Writes a version onto its stored row, so the row's optimistic lock version is the store's and stays in step. */
    @Override
    @Transactional
    public SubscriptionPlanVersion saveVersion(SubscriptionPlanVersion version) {
        SubscriptionPlanVersionJpaEntity stored = versions.findById(version.getId()).orElse(null);
        if (stored == null) {
            return versionMapper.toDomain(versions.save(versionMapper.toEntity(version)));
        }
        versionMapper.update(stored, version);
        return versionMapper.toDomain(versions.save(stored));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanOffer> findOffers(String planVersionId, String status) {
        return offers.findAllByPlanVersionIdAndStatus(planVersionId, status).stream().map(offerMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanOffer> findOfferById(String id) {
        return offers.findById(id).map(offerMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanPrice> findPrice(String id, String planId) {
        return prices.findByIdAndPlanId(id, planId).map(priceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanEntitlement> findEntitlements(String planVersionId) {
        return entitlements.findAllByPlanVersionIdOrderByFeatureKey(planVersionId).stream().map(entitlementMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommerceFeatureRegistry> findFeature(String featureKey) {
        return features.findById(featureKey).map(featureMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceFeatureRegistry> findFeatures(Collection<String> featureKeys) {
        return features.findAllById(featureKeys).stream().map(featureMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanVersionTranslation> findTranslation(String planVersionId, String locale) {
        return translations.findByPlanVersionIdAndLocale(planVersionId, locale).map(translationMapper::toDomain);
    }
}
