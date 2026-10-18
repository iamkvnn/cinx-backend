package com.cinx.user.policy.application.query;

import com.cinx.user.policy.domain.PolicyDocument;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;

import java.time.LocalDateTime;

/** A policy version without its sections, for lists. */
public record PolicySummaryView(
        String id,
        PolicyType policyType,
        String slug,
        String title,
        String summary,
        PolicyStatus status,
        Integer versionNumber,
        LocalDateTime effectiveAt,
        LocalDateTime publishedAt,
        Integer displayOrder) {

    public static PolicySummaryView of(PolicyDocument document) {
        return new PolicySummaryView(document.getId(), document.getPolicyType(), document.getSlug(), document.getTitle(),
                document.getSummary(), document.getStatus(), document.getVersionNumber(), document.getEffectiveAt(),
                document.getPublishedAt(), document.getDisplayOrder());
    }
}
