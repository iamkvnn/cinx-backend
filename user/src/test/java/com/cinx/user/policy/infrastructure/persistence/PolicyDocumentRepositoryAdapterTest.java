package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.user.policy.domain.PolicyDocument;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyDocumentRepositoryAdapterTest {
    @Mock PolicyDocumentJpaRepository jpa;
    PolicyDocumentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PolicyDocumentRepositoryAdapter(jpa, new PolicyDocumentPersistenceMapper());
    }

    @Test
    void aDocumentWithoutAnIdIsInserted() {
        when(jpa.save(any(PolicyDocumentJpaEntity.class))).thenAnswer(invocation -> {
            PolicyDocumentJpaEntity entity = invocation.getArgument(0);
            entity.setId("new-id");
            return entity;
        });

        PolicyDocument saved = adapter.save(PolicyDocument.builder().policyType(PolicyType.TERMS).slug("terms").title("T")
                .status(PolicyStatus.DRAFT).versionNumber(1).build());

        assertThat(saved.getId()).isEqualTo("new-id");
        assertThat(saved.getSlug()).isEqualTo("terms");
    }

    @Test
    void aStoredDocumentIsUpdatedInPlaceSoTheStoredRowKeepsItsLockVersion() {
        PolicyDocumentJpaEntity stored = new PolicyDocumentJpaEntity();
        stored.setId("doc-1");
        stored.setVersion(7L);
        stored.setStatus(PolicyStatus.DRAFT);
        stored.setSlug("terms");
        when(jpa.findById("doc-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);

        PolicyDocument saved = adapter.save(PolicyDocument.builder().id("doc-1").version(7L).slug("terms")
                .status(PolicyStatus.PUBLISHED).versionNumber(1).build());

        verify(jpa).save(stored);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.PUBLISHED);
        assertThat(saved.getVersion()).isEqualTo(7L);
    }

    @Test
    void savingADocumentThatWasDeletedUnderUsFailsLoudly() {
        when(jpa.findById("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(PolicyDocument.builder().id("gone").build()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("gone");
    }

    @Test
    void theMaxVersionOfASlugWithNoDocumentsIsZero() {
        when(jpa.findMaxVersionNumberBySlug("new")).thenReturn(null);
        when(jpa.findMaxVersionNumberBySlug("old")).thenReturn(5);

        assertThat(adapter.findMaxVersionNumberBySlug("new")).isZero();
        assertThat(adapter.findMaxVersionNumberBySlug("old")).isEqualTo(5);
    }
}
