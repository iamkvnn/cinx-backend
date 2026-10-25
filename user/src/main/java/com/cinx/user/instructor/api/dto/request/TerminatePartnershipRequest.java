package com.cinx.user.instructor.api.dto.request;

import com.cinx.user.profile.domain.PartnershipTerminationReasonType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TerminatePartnershipRequest(
        @NotNull(message = "reasonType is required")
        PartnershipTerminationReasonType reasonType,

        @Size(max = 2000, message = "reasonDetail must not exceed 2000 characters")
        String reasonDetail
) {
}
