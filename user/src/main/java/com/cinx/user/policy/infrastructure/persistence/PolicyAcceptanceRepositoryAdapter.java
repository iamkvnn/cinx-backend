package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.user.policy.domain.PolicyAcceptance;
import com.cinx.user.policy.domain.PolicyAcceptanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class PolicyAcceptanceRepositoryAdapter implements PolicyAcceptanceRepository {
    private final PolicyAcceptanceJpaRepository jpa;

    @Override
    @Transactional
    public List<PolicyAcceptance> saveAll(List<PolicyAcceptance> acceptances) {
        List<PolicyAcceptanceJpaEntity> rows = acceptances.stream().map(acceptance -> {
            PolicyAcceptanceJpaEntity row = new PolicyAcceptanceJpaEntity();
            row.setUserId(acceptance.getUserId());
            row.setPolicyId(acceptance.getPolicyId());
            row.setPolicyVersion(acceptance.getPolicyVersion());
            row.setAcceptedAt(acceptance.getAcceptedAt());
            return row;
        }).toList();
        return jpa.saveAll(rows).stream().map(row -> PolicyAcceptance.builder().id(row.getId()).userId(row.getUserId())
                .policyId(row.getPolicyId()).policyVersion(row.getPolicyVersion()).acceptedAt(row.getAcceptedAt()).build()).toList();
    }
}
