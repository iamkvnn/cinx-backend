package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.catalog.application.query.PlanVersionRef;
import com.cinx.commerce.catalog.application.query.SaleableOffer;
import com.cinx.commerce.catalog.domain.PlanCatalogRepository;
import com.cinx.commerce.catalog.domain.PlanOffer;
import com.cinx.commerce.catalog.domain.PlanPrice;
import com.cinx.commerce.catalog.domain.SubscriptionPlan;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanCatalogLookupService implements PlanCatalogLookup {
    private final PlanCatalogRepository catalog;

    @Override
    @Transactional(readOnly = true)
    public SaleableOffer resolveSaleableOffer(String offerId, LocalDateTime now) {
        PlanOffer offer = catalog.findOfferById(offerId)
                .filter(value -> "ACTIVE".equalsIgnoreCase(value.getStatus()))
                .orElseThrow(() -> new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Offer is unavailable"));
        if (offer.getSalesStart() != null && offer.getSalesEnd() != null
                && !offer.getSalesStart().isBefore(offer.getSalesEnd())) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Offer sales window is invalid");
        }
        if ((offer.getSalesStart() != null && now.isBefore(offer.getSalesStart()))
                || (offer.getSalesEnd() != null && !now.isBefore(offer.getSalesEnd()))) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Offer is not currently saleable");
        }
        var version = catalog.findVersionById(offer.getPlanVersionId())
                .filter(value -> "PUBLISHED".equalsIgnoreCase(value.getPublicationStatus()))
                .orElseThrow(() -> new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                        "Offer is not attached to a published plan"));
        SubscriptionPlan plan = catalog.findPlanById(version.getPlanId())
                .filter(value -> "ACTIVE".equalsIgnoreCase(value.getLifecycleStatus()))
                .orElseThrow(() -> new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Plan is not active"));
        if (!plan.getId().equals(offer.getPlanId())) {
            throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Offer relation is invalid");
        }
        if (offer.getPriceId() != null) {
            PlanPrice price = catalog.findPrice(offer.getPriceId(), plan.getId())
                    .orElseThrow(() -> new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Offer price is unavailable"));
            CurrencyAmountValidator.toMinorAmount(price.getAmount(), price.getCurrency());
            if (!"RECONCILED".equalsIgnoreCase(price.getStatus())
                    || price.getProviderPriceRef() == null || price.getProviderPriceRef().isBlank()) {
                throw new ForbiddenException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE,
                        "Offer price has not been reconciled with the billing provider");
            }
        }
        return new SaleableOffer(offer.getId(), offer.getPriceId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> planCodeOfVersion(String planVersionId) {
        return catalog.findVersionById(planVersionId)
                .flatMap(version -> catalog.findPlanById(version.getPlanId()))
                .map(SubscriptionPlan::getCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean planExists(String planCode) {
        return catalog.findPlanByCode(planCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanVersionRef> findVersion(String planCode, long versionNumber) {
        return catalog.findPlanByCode(planCode).flatMap(plan -> catalog.findVersion(plan.getId(), versionNumber)
                .map(version -> new PlanVersionRef(version.getId(), plan.getId(), plan.getCode(), version.getVersionNumber())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanFeatureGrant> grantsOf(String planVersionId) {
        return catalog.findEntitlements(planVersionId).stream().map(PlanFeatureGrant::of).toList();
    }
}
