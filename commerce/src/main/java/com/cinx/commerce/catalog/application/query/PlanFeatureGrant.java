package com.cinx.commerce.catalog.application.query;

import com.cinx.commerce.catalog.domain.PlanEntitlement;

/** What one plan version grants for one feature: a switch or a quota. */
public record PlanFeatureGrant(String featureKey, String grantType, Boolean booleanValue, Long quotaLimit,
                               Boolean quotaUnlimited, String quotaUnit, String resetPolicy) {

    public static PlanFeatureGrant of(PlanEntitlement entitlement) {
        return new PlanFeatureGrant(entitlement.getFeatureKey(), entitlement.getGrantType(), entitlement.getBooleanValue(),
                entitlement.getQuotaLimit(), entitlement.getQuotaUnlimited(), entitlement.getQuotaUnit(), entitlement.getResetPolicy());
    }
}
