package com.cinx.user.policy.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDocumentTest {
    @Test
    void aDraftBecomesPublishedWithItsTimeAndThenArchived() {
        PolicyDocument document = PolicyDocument.builder().status(PolicyStatus.DRAFT).build();
        LocalDateTime at = LocalDateTime.of(2026, 8, 1, 10, 0);

        assertThat(document.isDraft()).isTrue();
        document.publish(at);
        assertThat(document.isPublished()).isTrue();
        assertThat(document.getPublishedAt()).isEqualTo(at);
        document.archive();
        assertThat(document.isArchived()).isTrue();
        assertThat(document.getPublishedAt()).isEqualTo(at); // archiving keeps when it went live
    }

    @Test
    void replacingSectionsDropsTheOldOnesAndCopiesTheNewList() {
        PolicySection old = PolicySection.builder().id("old").heading("Old").build();
        PolicySection fresh = PolicySection.builder().heading("Fresh").build();
        PolicyDocument document = PolicyDocument.builder().sections(new java.util.ArrayList<>(List.of(old))).build();
        List<PolicySection> replacement = new java.util.ArrayList<>(List.of(fresh));

        document.replaceSections(replacement);
        replacement.clear();

        assertThat(document.getSections()).containsExactly(fresh);
    }

    @Test
    void aNewDocumentStartsWithNoSections() {
        assertThat(PolicyDocument.builder().build().getSections()).isEmpty();
    }
}
