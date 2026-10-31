package com.cinx.learning.learningpath.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathRequest {
    @Schema(example = "React Developer Path")
    @NotBlank(message = "title must not be blank")
    private String title;
    @Schema(example = "Master React and Redux")
    private String description;
    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<LearningPathItemRequest> items;
}
