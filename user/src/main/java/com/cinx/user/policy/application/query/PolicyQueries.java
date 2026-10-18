package com.cinx.user.policy.application.query;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;

import java.util.List;

/** Port for the policy lists, so no document is loaded with its sections just to be listed. */
public interface PolicyQueries {
    /** The published versions, in display order and then newest first. */
    List<PolicySummaryView> findPublishedSummaries();

    /** Every version for the management screen; each filter is optional and they combine. */
    PageResult<PolicySummaryView> findAllForManagement(PolicyStatus status, PolicyType policyType, String query, PageQuery page);
}
