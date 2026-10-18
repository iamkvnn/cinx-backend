package com.cinx.user.policy.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One version of a policy (terms, privacy and so on): written as a draft, published once, then archived. Its sections
 * are part of it and stored with it.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class PolicyDocument {
    private final String id;
    /** The persistence version (optimistic lock); null until the document is first stored. */
    private Long version;
    private PolicyType policyType;
    private String slug;
    private String title;
    private String summary;
    private PolicyStatus status;
    private Integer versionNumber;
    private LocalDateTime effectiveAt;
    private LocalDateTime publishedAt;
    private Integer displayOrder;
    /** Stamped by the store; read-only here. */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<PolicySection> sections = new ArrayList<>();

    public boolean isDraft() {
        return status == PolicyStatus.DRAFT;
    }

    public boolean isPublished() {
        return status == PolicyStatus.PUBLISHED;
    }

    public boolean isArchived() {
        return status == PolicyStatus.ARCHIVED;
    }

    public void publish(LocalDateTime now) {
        status = PolicyStatus.PUBLISHED;
        publishedAt = now;
    }

    public void archive() {
        status = PolicyStatus.ARCHIVED;
    }

    /** Swaps every section for the given ones, which are new (no id) and so are stored as new rows. */
    public void replaceSections(List<PolicySection> replacement) {
        sections = new ArrayList<>(replacement);
    }
}
