package com.cinx.learning.assessment.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The full, ordered list of an assignment's criteria; an empty list removes them all. */
public record SaveCriteriaRequest(
        @NotNull @Size(max = 20) @Valid List<CriterionInput> criteria
) {
    public record CriterionInput(
            String id,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 500) String description,
            @NotNull @DecimalMin("0.5") @DecimalMax("10") Double maxPoints
    ) {
    }
}
