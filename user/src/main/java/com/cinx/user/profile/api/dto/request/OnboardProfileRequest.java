package com.cinx.user.profile.api.dto.request;

import com.cinx.user.profile.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OnboardProfileRequest(
        @NotBlank
        @Schema(example = "John Doe")
        String name,
        Gender gender,
        String phoneNumber,
        String bio,
        String cvFileKey,
        @Size(max = 100)
        @Schema(description = "Category ids the learner is interested in; sent to recommendation as the preference event")
        List<@NotBlank String> preferredCategoryIds,
        @Size(max = 20)
        @Schema(description = "Ids of the published TERMS and PRIVACY policies the learner agreed to")
        List<@NotBlank String> acceptedPolicyIds,
        @Valid
        LearnerProfileAnswersRequest learningProfile
) {
}
