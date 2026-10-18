package com.cinx.user.policy.application;

import com.cinx.common.paging.PageResult;
import com.cinx.user.policy.application.command.CreatePolicyCommand;
import com.cinx.user.policy.application.command.UpdatePolicyCommand;
import com.cinx.user.policy.application.query.PolicySummaryView;
import com.cinx.user.policy.application.query.PolicyView;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;

import java.util.List;

public interface IPolicyService {
    List<PolicySummaryView> findPublishedPolicies();

    PolicyView findPublishedPolicyBySlug(String slug);

    PolicyView findById(String id);

    PageResult<PolicySummaryView> findAllVersions(
            int page,
            int size,
            PolicyStatus status,
            PolicyType policyType,
            String query,
            String sort
    );

    PolicyView createDraft(CreatePolicyCommand command);

    PolicyView updateDraft(String id, UpdatePolicyCommand command);

    PolicyView publish(String id);

    PolicyView archive(String id);
}
