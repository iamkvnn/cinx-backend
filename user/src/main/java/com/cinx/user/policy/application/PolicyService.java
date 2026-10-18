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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PolicyService implements IPolicyService {
    private static final String RESERVED_VERSIONS_SLUG = "versions";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyQueries policyQueries;
    private final PolicyEvents policyEvents;

    @Override
    @Transactional(readOnly = true)
    public List<PolicySummaryView> findPublishedPolicies() {
        return policyQueries.findPublishedSummaries();
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyView findPublishedPolicyBySlug(String slug) {
        return PolicyView.of(findPublishedOrThrow(normalizeSlug(slug)));
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyView findById(String id) {
        return PolicyView.of(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PolicySummaryView> findAllVersions(
            int page,
            int size,
            PolicyStatus status,
            PolicyType policyType,
            String query,
            String sort
    ) {
        return policyQueries.findAllForManagement(status, policyType, normalizeQuery(query), new PageQuery(page, size, sort));
    }

    @Override
    @Transactional
    public PolicyView createDraft(CreatePolicyCommand command) {
        String slug = normalizeSlug(command.slug());
        ensureAllowedSlug(slug);
        PolicyDocument document = PolicyDocument.builder()
                .policyType(command.policyType())
                .slug(slug)
                .title(command.title().trim())
                .summary(trimToNull(command.summary()))
                .status(PolicyStatus.DRAFT)
                .versionNumber(policyDocumentRepository.findMaxVersionNumberBySlug(slug) + 1)
                .effectiveAt(command.effectiveAt())
                .displayOrder(command.displayOrder())
                .build();
        document.replaceSections(sections(command.sections()));
        return PolicyView.of(policyDocumentRepository.save(document));
    }

    @Override
    @Transactional
    public PolicyView updateDraft(String id, UpdatePolicyCommand command) {
        PolicyDocument document = findByIdOrThrow(id);
        if (!document.isDraft()) {
            throw new BadRequestException("Only draft policies can be updated");
        }
        if (command.policyType() != null) {
            document.setPolicyType(command.policyType());
        }
        document.setTitle(command.title().trim());
        document.setSummary(trimToNull(command.summary()));
        document.setEffectiveAt(command.effectiveAt());
        document.setDisplayOrder(command.displayOrder());
        document.replaceSections(sections(command.sections()));
        return PolicyView.of(policyDocumentRepository.save(document));
    }

    @Override
    @Transactional
    public PolicyView publish(String id) {
        PolicyDocument document = findByIdOrThrow(id);
        if (!document.isDraft()) {
            throw new BadRequestException("Only draft policies can be published");
        }
        policyDocumentRepository.findAllPublishedBySlug(document.getSlug()).stream()
                .filter(oldDocument -> !oldDocument.getId().equals(document.getId()))
                .forEach(oldDocument -> {
                    oldDocument.archive();
                    policyDocumentRepository.save(oldDocument);
                });
        document.publish(LocalDateTime.now());
        PolicyDocument saved = policyDocumentRepository.save(document);
        publishPolicyPublished(saved);
        return PolicyView.of(saved);
    }

    @Override
    @Transactional
    public PolicyView archive(String id) {
        PolicyDocument document = findByIdOrThrow(id);
        if (document.isArchived()) {
            return PolicyView.of(document);
        }
        boolean wasPublished = document.isPublished();
        document.archive();
        PolicyDocument saved = policyDocumentRepository.save(document);
        if (wasPublished) {
            publishPolicyArchived(saved);
        }
        return PolicyView.of(saved);
    }

    private PolicyDocument findPublishedOrThrow(String slug) {
        return policyDocumentRepository.findLatestPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Published policy not found"));
    }

    private PolicyDocument findByIdOrThrow(String id) {
        return policyDocumentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Policy not found"));
    }

    /** Orders the sections, trims their text and gives each a unique anchor within the document. */
    private List<PolicySection> sections(List<PolicySectionCommand> commands) {
        Set<String> anchors = new HashSet<>();
        return commands.stream()
                .sorted(Comparator.comparing(PolicySectionCommand::orderIndex))
                .map(command -> PolicySection.builder()
                        .heading(command.heading().trim())
                        .anchor(uniqueAnchor(command, anchors))
                        .bodyMarkdown(command.bodyMarkdown().trim())
                        .orderIndex(command.orderIndex())
                        .build())
                .toList();
    }

    private String uniqueAnchor(PolicySectionCommand command, Set<String> existingAnchors) {
        String base = trimToNull(command.anchor());
        if (base == null) {
            base = slugify(command.heading());
        } else {
            base = slugify(base);
        }
        if (base == null || base.isBlank()) {
            base = "section";
        }
        String candidate = base;
        int counter = 2;
        while (!existingAnchors.add(candidate)) {
            candidate = base + "-" + counter;
            counter++;
        }
        return candidate;
    }

    private String normalizeSlug(String value) {
        String slug = slugify(value);
        if (slug == null || slug.isBlank()) {
            throw new BadRequestException("Policy slug is required");
        }
        return slug;
    }

    private String slugify(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replace("đ", "d")
                .replace("Đ", "D");
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String slug = NON_SLUG_CHARS.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isBlank() ? null : slug;
    }

    private void ensureAllowedSlug(String slug) {
        if (RESERVED_VERSIONS_SLUG.equals(slug)) {
            throw new BadRequestException("Policy slug is reserved");
        }
    }

    private String normalizeQuery(String query) {
        return trimToNull(query);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void publishPolicyPublished(PolicyDocument document) {
        policyEvents.publishPolicyPublished(
                policyDocumentId(document),
                document.getTitle(),
                policySourceUrl(document),
                flattenForRag(document),
                document.getVersionNumber(),
                document.getPublishedAt()
        );
    }

    private void publishPolicyArchived(PolicyDocument document) {
        policyEvents.publishPolicyArchived(
                policyDocumentId(document),
                policySourceUrl(document),
                document.getPublishedAt()
        );
    }

    private String policyDocumentId(PolicyDocument document) {
        return "policy-" + document.getSlug();
    }

    private String policySourceUrl(PolicyDocument document) {
        return "/policies/" + document.getSlug();
    }

    private String flattenForRag(PolicyDocument document) {
        StringBuilder builder = new StringBuilder();
        builder.append(document.getTitle()).append("\n\n");
        if (document.getSummary() != null) {
            builder.append(document.getSummary()).append("\n\n");
        }
        document.getSections().stream()
                .sorted(Comparator.comparing(PolicySection::getOrderIndex))
                .forEach(section -> builder
                        .append(section.getHeading())
                        .append("\n")
                        .append(section.getBodyMarkdown())
                        .append("\n\n"));
        return builder.toString().trim();
    }
}
