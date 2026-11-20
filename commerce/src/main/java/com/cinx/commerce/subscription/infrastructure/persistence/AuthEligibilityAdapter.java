package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.application.AuthEligibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AuthEligibilityAdapter implements AuthEligibility {
    private final AuthEligibilityProjectionRepository projections;

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String userId) {
        return projections.findById(userId).map(projection -> "ACTIVE".equalsIgnoreCase(projection.getAccountStatus())).orElse(true);
    }
}
