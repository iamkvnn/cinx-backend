package com.cinx.learning.entitlement.infrastructure.persistence;

import com.cinx.learning.entitlement.application.SubscriptionFeatureAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
class SubscriptionFeatureAccessAdapter implements SubscriptionFeatureAccess {
    private final UserEntitlementProjectionRepository projectionRepository;
    private final UserFeatureEntitlementRepository featureRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasFeature(String userId, String featureKey, LocalDateTime now) {
        if (featureKey == null || featureKey.isBlank()) {
            return false;
        }
        UserEntitlementProjection projection = projectionRepository.findById(userId).orElse(null);
        if (projection == null || !("ACTIVE".equalsIgnoreCase(projection.getAccessStatus())
                || "GRACE".equalsIgnoreCase(projection.getAccessStatus()))) {
            return false;
        }
        if (projection.getEffectiveUntil() != null && !projection.getEffectiveUntil().isAfter(now)) {
            return false;
        }
        if (projection.getReconcileBy() != null && !projection.getReconcileBy().isAfter(now)) {
            return false;
        }
        UserFeatureEntitlement feature = featureRepository.findByUserIdAndFeatureKey(userId, featureKey).orElse(null);
        return feature != null && Boolean.TRUE.equals(feature.getEnabled())
                && (feature.getEffectiveUntil() == null || feature.getEffectiveUntil().isAfter(now));
    }
}
