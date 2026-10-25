package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.common.mapper.SortConverter;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.user.policy.application.query.PolicyQueries;
import com.cinx.user.policy.application.query.PolicySummaryView;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PolicyQueriesAdapter implements PolicyQueries {
    private final PolicyDocumentJpaRepository jpa;

    @Override
    public List<PolicySummaryView> findPublishedSummaries() {
        return jpa.findAllByStatusOrderByDisplayOrderAscPublishedAtDesc(PolicyStatus.PUBLISHED).stream()
                .map(PolicyQueriesAdapter::summary).toList();
    }

    @Override
    public PageResult<PolicySummaryView> findAllForManagement(PolicyStatus status, PolicyType policyType, String query, PageQuery page) {
        Page<PolicyDocumentJpaEntity> rows = jpa.findAllForManagement(status, policyType, query,
                PageRequest.of(page.page() - 1, page.size(), SortConverter.toSort(page.sort())));
        return new PageResult<>(rows.getContent().stream().map(PolicyQueriesAdapter::summary).toList(),
                page.page(), page.size(), rows.getTotalElements());
    }

    private static PolicySummaryView summary(PolicyDocumentJpaEntity entity) {
        return new PolicySummaryView(entity.getId(), entity.getPolicyType(), entity.getSlug(), entity.getTitle(), entity.getSummary(),
                entity.getStatus(), entity.getVersionNumber(), entity.getEffectiveAt(), entity.getPublishedAt(), entity.getDisplayOrder());
    }
}
