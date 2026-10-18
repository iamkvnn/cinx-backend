package com.cinx.user.policy.application.query;

import com.cinx.user.policy.domain.PolicyDocument;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;

import java.time.LocalDateTime;
import java.util.List;

/** A policy version with its sections, as a reader sees it. */
public record PolicyView(
        String id,
        PolicyType policyType,
        String slug,
        String title,
        String summary,
        PolicyStatus status,
        Integer versionNumber,
        LocalDateTime effectiveAt,
        LocalDateTime publishedAt,
        Integer displayOrder,
        List<PolicySectionView> sections) {

    public static PolicyView of(PolicyDocument document) {
        return new PolicyView(document.getId(), document.getPolicyType(), document.getSlug(), document.getTitle(),
                document.getSummary(), document.getStatus(), document.getVersionNumber(), document.getEffectiveAt(),
                document.getPublishedAt(), document.getDisplayOrder(),
                document.getSections().stream().map(section -> new PolicySectionView(section.getId(), section.getHeading(),
                        section.getAnchor(), section.getBodyMarkdown(), section.getOrderIndex())).toList());
    }
}
