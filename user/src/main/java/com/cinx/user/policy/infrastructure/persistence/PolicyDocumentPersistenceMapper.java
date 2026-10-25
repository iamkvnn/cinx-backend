package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.user.policy.domain.PolicyDocument;
import com.cinx.user.policy.domain.PolicySection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Maps a policy and its sections in both directions; written by hand because the sections are a child collection. */
@Component
class PolicyDocumentPersistenceMapper {

    PolicyDocument toDomain(PolicyDocumentJpaEntity entity) {
        List<PolicySection> sections = new ArrayList<>();
        entity.getSections().forEach(section -> sections.add(PolicySection.builder().id(section.getId())
                .heading(section.getHeading()).anchor(section.getAnchor()).bodyMarkdown(section.getBodyMarkdown())
                .orderIndex(section.getOrderIndex()).build()));
        return PolicyDocument.builder().id(entity.getId()).version(entity.getVersion())
                .policyType(entity.getPolicyType()).slug(entity.getSlug()).title(entity.getTitle()).summary(entity.getSummary())
                .status(entity.getStatus()).versionNumber(entity.getVersionNumber()).effectiveAt(entity.getEffectiveAt())
                .publishedAt(entity.getPublishedAt()).displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).sections(sections).build();
    }

    PolicyDocumentJpaEntity toEntity(PolicyDocument document) {
        PolicyDocumentJpaEntity entity = new PolicyDocumentJpaEntity();
        entity.setVersion(document.getVersion());
        copyFields(entity, document);
        replaceSections(entity, document.getSections());
        return entity;
    }

    /**
     * Writes the domain's changes onto the stored row. The sections are rebuilt only when the document carries a section
     * that has no id yet (it was just replaced); otherwise the stored sections are left exactly as they are, so
     * publishing or archiving a policy never rewrites its text.
     */
    void update(PolicyDocumentJpaEntity entity, PolicyDocument document) {
        copyFields(entity, document);
        if (document.getSections().stream().anyMatch(section -> section.getId() == null)) {
            replaceSections(entity, document.getSections());
        }
    }

    private void copyFields(PolicyDocumentJpaEntity entity, PolicyDocument document) {
        entity.setPolicyType(document.getPolicyType());
        entity.setSlug(document.getSlug());
        entity.setTitle(document.getTitle());
        entity.setSummary(document.getSummary());
        entity.setStatus(document.getStatus());
        entity.setVersionNumber(document.getVersionNumber());
        entity.setEffectiveAt(document.getEffectiveAt());
        entity.setPublishedAt(document.getPublishedAt());
        entity.setDisplayOrder(document.getDisplayOrder());
    }

    private void replaceSections(PolicyDocumentJpaEntity entity, List<PolicySection> sections) {
        entity.getSections().clear();
        sections.forEach(section -> {
            PolicySectionJpaEntity row = new PolicySectionJpaEntity();
            row.setDocument(entity);
            row.setHeading(section.getHeading());
            row.setAnchor(section.getAnchor());
            row.setBodyMarkdown(section.getBodyMarkdown());
            row.setOrderIndex(section.getOrderIndex());
            entity.getSections().add(row);
        });
    }
}
