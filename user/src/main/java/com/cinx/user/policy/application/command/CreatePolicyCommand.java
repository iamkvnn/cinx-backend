package com.cinx.user.policy.application.command;

import com.cinx.user.policy.domain.PolicyType;

import java.time.LocalDateTime;
import java.util.List;

public record CreatePolicyCommand(
        PolicyType policyType,
        String slug,
        String title,
        String summary,
        LocalDateTime effectiveAt,
        Integer displayOrder,
        List<PolicySectionCommand> sections) {
}
