package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.catalog.application.query.PlanVersionRef;
import com.cinx.commerce.catalog.application.query.SaleableOffer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** What the subscriptions may ask the plan catalog. */
public interface PlanCatalogLookup {
    /**
     * The offer when it can be bought at {@code now}: active, inside its sales window, on a published version of an
     * active plan, and with a reconciled price when it is not free.
     *
     * @throws com.cinx.common.exception.ForbiddenException with PLAN_OR_PRICE_UNAVAILABLE and the reason otherwise
     */
    SaleableOffer resolveSaleableOffer(String offerId, LocalDateTime now);

    /** The code of the plan a plan version belongs to. */
    Optional<String> planCodeOfVersion(String planVersionId);

    boolean planExists(String planCode);

    Optional<PlanVersionRef> findVersion(String planCode, long versionNumber);

    /** What the plan version grants, in feature-key order. */
    List<PlanFeatureGrant> grantsOf(String planVersionId);
}
