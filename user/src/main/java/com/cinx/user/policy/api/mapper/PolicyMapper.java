package com.cinx.user.policy.api.mapper;

import com.cinx.user.policy.api.dto.request.CreatePolicyRequest;
import com.cinx.user.policy.api.dto.request.PolicySectionRequest;
import com.cinx.user.policy.api.dto.request.UpdatePolicyRequest;
import com.cinx.user.policy.api.dto.response.PolicyDetailResponse;
import com.cinx.user.policy.api.dto.response.PolicySectionResponse;
import com.cinx.user.policy.api.dto.response.PolicySummaryResponse;
import com.cinx.user.policy.application.command.CreatePolicyCommand;
import com.cinx.user.policy.application.command.PolicySectionCommand;
import com.cinx.user.policy.application.command.UpdatePolicyCommand;
import com.cinx.user.policy.application.query.PolicySectionView;
import com.cinx.user.policy.application.query.PolicySummaryView;
import com.cinx.user.policy.application.query.PolicyView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PolicyMapper {
    PolicySummaryResponse toSummaryResponse(PolicySummaryView view);

    PolicyDetailResponse toDetailResponse(PolicyView view);

    PolicySectionResponse toSectionResponse(PolicySectionView view);

    CreatePolicyCommand toCommand(CreatePolicyRequest request);

    UpdatePolicyCommand toCommand(UpdatePolicyRequest request);

    PolicySectionCommand toCommand(PolicySectionRequest request);
}
