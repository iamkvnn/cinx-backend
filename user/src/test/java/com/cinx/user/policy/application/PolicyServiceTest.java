package com.cinx.user.policy.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.user.policy.application.command.CreatePolicyCommand;
import com.cinx.user.policy.application.command.PolicySectionCommand;
import com.cinx.user.policy.application.command.UpdatePolicyCommand;
import com.cinx.user.policy.application.query.PolicyQueries;
import com.cinx.user.policy.application.query.PolicySummaryView;
import com.cinx.user.policy.application.query.PolicyView;
import com.cinx.user.policy.domain.PolicyDocument;
import com.cinx.user.policy.domain.PolicyDocumentRepository;
import com.cinx.user.policy.domain.PolicySection;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {
    @Mock PolicyDocumentRepository repository;
    @Mock PolicyQueries queries;
    @Mock PolicyEvents events;
    PolicyService service;

    @BeforeEach
    void setUp() {
        service = new PolicyService(repository, queries, events);
    }

    // ---- creating a draft

    @Test
    void aDraftGetsANormalizedSlugTheNextVersionNumberAndOrderedUniqueAnchors() {
        when(repository.findMaxVersionNumberBySlug("chinh-sach-bao-mat")).thenReturn(2);
        when(repository.save(any(PolicyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyView view = service.createDraft(new CreatePolicyCommand(PolicyType.PRIVACY, "  Chính sách Bảo mật! ", "  Privacy  ", "   ", null, 1, List.of(
                new PolicySectionCommand("Second", null, " body two ", 2),
                new PolicySectionCommand(" Dữ liệu ", null, " body one ", 1),
                new PolicySectionCommand("Dữ liệu", null, "body three", 3))));

        assertThat(view.slug()).isEqualTo("chinh-sach-bao-mat");
        assertThat(view.versionNumber()).isEqualTo(3); // highest stored (2) + 1
        assertThat(view.status()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(view.title()).isEqualTo("Privacy");
        assertThat(view.summary()).isNull(); // blank summary is stored as null
        assertThat(view.sections()).extracting("heading", "anchor", "bodyMarkdown", "orderIndex").containsExactly(
                org.assertj.core.groups.Tuple.tuple("Dữ liệu", "du-lieu", "body one", 1),
                org.assertj.core.groups.Tuple.tuple("Second", "second", "body two", 2),
                org.assertj.core.groups.Tuple.tuple("Dữ liệu", "du-lieu-2", "body three", 3)); // same heading → suffix
    }

    @Test
    void aFirstDraftOfASlugStartsAtVersionOne() {
        when(repository.findMaxVersionNumberBySlug("terms")).thenReturn(0);
        when(repository.save(any(PolicyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createDraft(new CreatePolicyCommand(PolicyType.TERMS, "terms", "Terms", null, null, 0, List.of())).versionNumber()).isEqualTo(1);
    }

    @Test
    void anExplicitAnchorIsSlugifiedAndAnEmptyOneFallsBackToSection() {
        when(repository.findMaxVersionNumberBySlug("terms")).thenReturn(0);
        when(repository.save(any(PolicyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyView view = service.createDraft(new CreatePolicyCommand(PolicyType.TERMS, "terms", "Terms", null, null, 0, List.of(
                new PolicySectionCommand("Heading", "My Anchor Đ", "b", 1),
                new PolicySectionCommand("!!!", null, "b", 2))));

        assertThat(view.sections()).extracting("anchor").containsExactly("my-anchor-d", "section");
    }

    @Test
    void theSlugVersionsIsReservedAndBlankSlugsAreRefused() {
        assertThatThrownBy(() -> service.createDraft(new CreatePolicyCommand(PolicyType.TERMS, " Versions ", "T", null, null, 0, List.of())))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("reserved");
        assertThatThrownBy(() -> service.createDraft(new CreatePolicyCommand(PolicyType.TERMS, " !! ", "T", null, null, 0, List.of())))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("slug is required");
        assertThatThrownBy(() -> service.createDraft(new CreatePolicyCommand(PolicyType.TERMS, null, "T", null, null, 0, List.of())))
                .isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
    }

    // ---- updating a draft

    @Test
    void onlyADraftCanBeUpdated() {
        for (PolicyStatus status : List.of(PolicyStatus.PUBLISHED, PolicyStatus.ARCHIVED)) {
            when(repository.findById("doc-" + status)).thenReturn(Optional.of(document("doc-" + status, "terms", status, 1)));
            assertThatThrownBy(() -> service.updateDraft("doc-" + status, update(null))).as(status.name()).isInstanceOf(BadRequestException.class);
        }
        verify(repository, never()).save(any());
    }

    @Test
    void updatingADraftReplacesItsContentAndKeepsTheTypeWhenNoneIsSent() {
        PolicyDocument draft = document("doc-1", "terms", PolicyStatus.DRAFT, 1);
        draft.setPolicyType(PolicyType.TERMS);
        draft.replaceSections(List.of(PolicySection.builder().id("old").heading("Old").anchor("old").bodyMarkdown("x").orderIndex(1).build()));
        when(repository.findById("doc-1")).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        PolicyView view = service.updateDraft("doc-1", update(null));

        assertThat(view.policyType()).isEqualTo(PolicyType.TERMS);
        assertThat(view.title()).isEqualTo("New title");
        assertThat(view.sections()).hasSize(1);
        assertThat(view.sections().get(0).id()).isNull(); // replaced sections are new rows
        assertThat(view.sections().get(0).heading()).isEqualTo("Fresh");
    }

    @Test
    void updatingWithANewTypeChangesIt() {
        PolicyDocument draft = document("doc-1", "terms", PolicyStatus.DRAFT, 1);
        draft.setPolicyType(PolicyType.TERMS);
        when(repository.findById("doc-1")).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        assertThat(service.updateDraft("doc-1", update(PolicyType.REFUND)).policyType()).isEqualTo(PolicyType.REFUND);
    }

    @Test
    void updatingAnUnknownPolicyIsNotFound() {
        when(repository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDraft("ghost", update(null))).isInstanceOf(NotFoundException.class);
    }

    // ---- publishing

    @Test
    void publishingArchivesTheOlderPublishedVersionsAndAnnouncesTheNewOneWithFlattenedContent() {
        PolicyDocument draft = document("doc-3", "terms", PolicyStatus.DRAFT, 3);
        draft.setTitle("Terms of use");
        draft.setSummary("Short summary");
        draft.replaceSections(List.of(
                PolicySection.builder().heading("B").anchor("b").bodyMarkdown("second body").orderIndex(2).build(),
                PolicySection.builder().heading("A").anchor("a").bodyMarkdown("first body").orderIndex(1).build()));
        PolicyDocument old = document("doc-2", "terms", PolicyStatus.PUBLISHED, 2);
        when(repository.findById("doc-3")).thenReturn(Optional.of(draft));
        when(repository.findAllPublishedBySlug("terms")).thenReturn(List.of(old, draft));
        when(repository.save(any(PolicyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyView view = service.publish("doc-3");

        assertThat(old.isArchived()).isTrue();
        assertThat(view.status()).isEqualTo(PolicyStatus.PUBLISHED);
        assertThat(view.publishedAt()).isNotNull();
        InOrder order = inOrder(repository, events);
        order.verify(repository).save(old);
        order.verify(repository).save(draft);
        order.verify(events).publishPolicyPublished(eq("policy-terms"), eq("Terms of use"), eq("/policies/terms"),
                eq("Terms of use\n\nShort summary\n\nA\nfirst body\n\nB\nsecond body"), eq(3), eq(view.publishedAt()));
    }

    @Test
    void onlyADraftCanBePublished() {
        when(repository.findById("doc-1")).thenReturn(Optional.of(document("doc-1", "terms", PolicyStatus.PUBLISHED, 1)));

        assertThatThrownBy(() -> service.publish("doc-1")).isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
        verify(events, never()).publishPolicyPublished(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    // ---- archiving

    @Test
    void archivingAPublishedPolicyAnnouncesItsRetirement() {
        PolicyDocument published = document("doc-1", "terms", PolicyStatus.PUBLISHED, 1);
        LocalDateTime publishedAt = LocalDateTime.of(2026, 7, 1, 9, 0);
        published.setPublishedAt(publishedAt);
        when(repository.findById("doc-1")).thenReturn(Optional.of(published));
        when(repository.save(published)).thenReturn(published);

        assertThat(service.archive("doc-1").status()).isEqualTo(PolicyStatus.ARCHIVED);

        verify(events).publishPolicyArchived("policy-terms", "/policies/terms", publishedAt);
    }

    @Test
    void archivingADraftAnnouncesNothingBecauseItNeverWentLive() {
        PolicyDocument draft = document("doc-1", "terms", PolicyStatus.DRAFT, 1);
        when(repository.findById("doc-1")).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        assertThat(service.archive("doc-1").status()).isEqualTo(PolicyStatus.ARCHIVED);

        verify(events, never()).publishPolicyArchived(any(), any(), any());
    }

    @Test
    void archivingAnArchivedPolicyChangesNothing() {
        when(repository.findById("doc-1")).thenReturn(Optional.of(document("doc-1", "terms", PolicyStatus.ARCHIVED, 1)));

        assertThat(service.archive("doc-1").status()).isEqualTo(PolicyStatus.ARCHIVED);

        verify(repository, never()).save(any());
        verify(events, never()).publishPolicyArchived(any(), any(), any());
    }

    // ---- reading

    @Test
    void readingAPublishedPolicyNormalizesTheSlugAndHidesUnpublishedOnes() {
        when(repository.findLatestPublishedBySlug("chinh-sach")).thenReturn(Optional.of(document("doc-1", "chinh-sach", PolicyStatus.PUBLISHED, 1)));
        when(repository.findLatestPublishedBySlug("missing")).thenReturn(Optional.empty());

        assertThat(service.findPublishedPolicyBySlug(" Chính Sách ").id()).isEqualTo("doc-1");
        assertThatThrownBy(() -> service.findPublishedPolicyBySlug("missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void managementListingTrimsTheQueryAndPassesThePage() {
        PageResult<PolicySummaryView> page = new PageResult<>(List.of(), 1, 10, 0);
        when(queries.findAllForManagement(PolicyStatus.DRAFT, PolicyType.TERMS, "privacy", new PageQuery(1, 10, "{}"))).thenReturn(page);
        when(queries.findAllForManagement(null, null, null, new PageQuery(1, 10, null))).thenReturn(page);

        assertThat(service.findAllVersions(1, 10, PolicyStatus.DRAFT, PolicyType.TERMS, "  privacy ", "{}")).isSameAs(page);
        assertThat(service.findAllVersions(1, 10, null, null, "   ", null)).isSameAs(page);
    }

    private static UpdatePolicyCommand update(PolicyType type) {
        return new UpdatePolicyCommand(type, " New title ", null, null, 4, List.of(new PolicySectionCommand("Fresh", null, "body", 1)));
    }

    private static PolicyDocument document(String id, String slug, PolicyStatus status, int versionNumber) {
        return PolicyDocument.builder().id(id).slug(slug).title("Title").status(status).versionNumber(versionNumber).build();
    }
}
