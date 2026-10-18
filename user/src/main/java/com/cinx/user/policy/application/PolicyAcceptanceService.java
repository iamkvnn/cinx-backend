package com.cinx.user.policy.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.user.policy.application.query.PolicyQueries;
import com.cinx.user.policy.application.query.PolicySummaryView;
import com.cinx.user.policy.domain.PolicyAcceptance;
import com.cinx.user.policy.domain.PolicyAcceptanceRepository;
import com.cinx.user.policy.domain.PolicyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PolicyAcceptanceService implements PolicyAcceptances {
    private final PolicyQueries policyQueries;
    private final PolicyAcceptanceRepository acceptances;

    @Override
    public List<AcceptedPolicy> resolveAccepted(List<String> requestedIds) {
        List<PolicySummaryView> published = policyQueries.findPublishedSummaries();
        Set<String> ids = new HashSet<>(distinctIds(requestedIds));
        List<PolicySummaryView> accepted = published.stream().filter(policy -> ids.contains(policy.id())).toList();
        if (accepted.size() != ids.size()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "acceptedPolicyIds must be published policies");
        }
        for (PolicyType required : List.of(PolicyType.TERMS, PolicyType.PRIVACY)) {
            boolean offered = published.stream().anyMatch(policy -> policy.policyType() == required);
            boolean agreed = accepted.stream().anyMatch(policy -> policy.policyType() == required);
            if (offered && !agreed) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST,
                        "Agreement to the published " + required + " policy is required");
            }
        }
        return accepted.stream().map(policy -> new AcceptedPolicy(policy.id(), policy.versionNumber(), policy.policyType())).toList();
    }

    @Override
    public void record(String userId, List<AcceptedPolicy> accepted, LocalDateTime acceptedAt) {
        acceptances.saveAll(accepted.stream()
                .map(policy -> PolicyAcceptance.builder().userId(userId).policyId(policy.policyId())
                        .policyVersion(policy.versionNumber()).acceptedAt(acceptedAt).build())
                .toList());
    }

    private static List<String> distinctIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
    }
}
