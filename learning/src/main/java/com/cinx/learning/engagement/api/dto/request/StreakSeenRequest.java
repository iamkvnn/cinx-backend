package com.cinx.learning.engagement.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Exactly one of the two: the milestone just celebrated, or that the lost streak was shown. */
public record StreakSeenRequest(
    @Schema(example = "7", nullable = true)
    Integer milestone,

    @Schema(example = "true", nullable = true)
    Boolean lost
) {}
