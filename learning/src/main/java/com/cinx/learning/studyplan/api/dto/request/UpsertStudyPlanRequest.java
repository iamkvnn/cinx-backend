package com.cinx.learning.studyplan.api.dto.request;

import com.cinx.learning.studyplan.domain.StudyPlanSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertStudyPlanRequest(
        @NotNull
        @Schema(example = "PATH", description = "PATH = the active personal path; COURSES = courseIds in this order")
        StudyPlanSource source,

        @Size(max = 12, message = "A study plan has at most 12 courses")
        @Schema(description = "Owned course ids in learning order; required for COURSES, ignored for PATH")
        List<@NotBlank String> courseIds,

        @NotEmpty @Size(max = 7)
        @Schema(example = "[1,2,4,5,6]", description = "ISO weekdays, 1 = Monday … 7 = Sunday")
        List<@NotNull @Min(1) @Max(7) Integer> freeDays,

        @NotNull @Min(15) @Max(120)
        @Schema(example = "30")
        Integer minutesPerDay
) {
}
