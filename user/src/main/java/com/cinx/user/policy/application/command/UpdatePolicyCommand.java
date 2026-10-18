package com.cinx.user.policy.application.command;

import com.cinx.user.policy.domain.PolicyType;

import java.time.LocalDateTime;
import java.util.List;

/** New content for a draft; a null {@code policyType} keeps the type it has. */
public record UpdatePolicyCommand(
        PolicyType policyType,
        String title,
        String summary,
        LocalDateTime effectiveAt,
        Integer displayOrder,
        List<PolicySectionCommand> sections) {
}
