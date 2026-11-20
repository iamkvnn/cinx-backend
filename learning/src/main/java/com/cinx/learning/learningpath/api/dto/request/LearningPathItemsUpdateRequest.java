package com.cinx.learning.learningpath.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Replaces the lessons of an active or pending path, in this order. Title and description change only when sent. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemsUpdateRequest {
    @Schema(example = "Backend Java trong 4 tháng")
    @Size(max = 255)
    private String title;
    @Size(max = 2000)
    private String description;
    @Schema(description = "The lessons in path order; orderIndex is ignored and assigned from this order")
    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<LearningPathItemRequest> items;
}
