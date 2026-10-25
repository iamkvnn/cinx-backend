package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.user.policy.domain.PolicyDocument;
import com.cinx.user.policy.domain.PolicySection;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDocumentPersistenceMapperTest {
    PolicyDocumentPersistenceMapper mapper = new PolicyDocumentPersistenceMapper();

    @Test
    void aStoredDocumentAndItsSectionsComeBackAsTheDomainModel() {
        PolicyDocumentJpaEntity entity = storedDocument();

        PolicyDocument document = mapper.toDomain(entity);

        assertThat(document.getId()).isEqualTo("doc-1");
        assertThat(document.getVersion()).isEqualTo(4L);
        assertThat(document.getPolicyType()).isEqualTo(PolicyType.TERMS);
        assertThat(document.getSlug()).isEqualTo("terms");
        assertThat(document.getStatus()).isEqualTo(PolicyStatus.PUBLISHED);
        assertThat(document.getVersionNumber()).isEqualTo(2);
        assertThat(document.getSections()).extracting(PolicySection::getId, PolicySection::getHeading, PolicySection::getAnchor,
                PolicySection::getBodyMarkdown, PolicySection::getOrderIndex).containsExactly(
                org.assertj.core.groups.Tuple.tuple("sec-1", "Heading", "heading", "Body", 1));
    }

    @Test
    void aNewDocumentBecomesAnEntityWithOneChildRowPerSection() {
        PolicyDocument document = PolicyDocument.builder().policyType(PolicyType.PRIVACY).slug("privacy").title("Privacy")
                .status(PolicyStatus.DRAFT).versionNumber(1).displayOrder(3)
                .sections(List.of(PolicySection.builder().heading("H").anchor("h").bodyMarkdown("B").orderIndex(1).build())).build();

        PolicyDocumentJpaEntity entity = mapper.toEntity(document);

        assertThat(entity.getSlug()).isEqualTo("privacy");
        assertThat(entity.getStatus()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(entity.getDisplayOrder()).isEqualTo(3);
        assertThat(entity.getSections()).hasSize(1);
        assertThat(entity.getSections().get(0).getDocument()).isSameAs(entity);
        assertThat(entity.getSections().get(0).getBodyMarkdown()).isEqualTo("B");
    }

    @Test
    void publishingChangesTheStatusButLeavesTheStoredSectionRowsUntouched() {
        PolicyDocumentJpaEntity entity = storedDocument();
        PolicySectionJpaEntity storedRow = entity.getSections().get(0);
        PolicyDocument document = mapper.toDomain(entity);
        document.archive();

        mapper.update(entity, document);

        assertThat(entity.getStatus()).isEqualTo(PolicyStatus.ARCHIVED);
        assertThat(entity.getSections()).hasSize(1);
        assertThat(entity.getSections().get(0)).isSameAs(storedRow); // the same row, not a rewritten copy
        assertThat(entity.getVersion()).isEqualTo(4L); // the lock version is the store's, not overwritten
    }

    @Test
    void replacedSectionsWithoutAnIdRebuildTheChildRows() {
        PolicyDocumentJpaEntity entity = storedDocument();
        PolicySectionJpaEntity storedRow = entity.getSections().get(0);
        PolicyDocument document = mapper.toDomain(entity);
        document.replaceSections(List.of(
                PolicySection.builder().heading("New 1").anchor("new-1").bodyMarkdown("b1").orderIndex(1).build(),
                PolicySection.builder().heading("New 2").anchor("new-2").bodyMarkdown("b2").orderIndex(2).build()));

        mapper.update(entity, document);

        assertThat(entity.getSections()).extracting(PolicySectionJpaEntity::getHeading).containsExactly("New 1", "New 2");
        assertThat(entity.getSections()).doesNotContain(storedRow);
        assertThat(entity.getSections()).allSatisfy(row -> assertThat(row.getDocument()).isSameAs(entity));
    }

    private static PolicyDocumentJpaEntity storedDocument() {
        PolicyDocumentJpaEntity entity = new PolicyDocumentJpaEntity();
        entity.setId("doc-1");
        entity.setVersion(4L);
        entity.setPolicyType(PolicyType.TERMS);
        entity.setSlug("terms");
        entity.setTitle("Terms");
        entity.setStatus(PolicyStatus.PUBLISHED);
        entity.setVersionNumber(2);
        entity.setPublishedAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        PolicySectionJpaEntity section = new PolicySectionJpaEntity();
        section.setId("sec-1");
        section.setDocument(entity);
        section.setHeading("Heading");
        section.setAnchor("heading");
        section.setBodyMarkdown("Body");
        section.setOrderIndex(1);
        entity.getSections().add(section);
        return entity;
    }
}
