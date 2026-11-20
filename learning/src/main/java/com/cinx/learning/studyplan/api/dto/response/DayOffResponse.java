package com.cinx.learning.studyplan.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** After marking a day busy: how many planned sessions moved, and the plan's current week. */
public record DayOffResponse(
        @Schema(example = "3") int moved,
        StudyPlanResponse plan
) {
}
