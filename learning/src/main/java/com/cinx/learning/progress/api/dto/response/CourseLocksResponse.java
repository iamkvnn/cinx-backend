package com.cinx.learning.progress.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record CourseLocksResponse(
        @Schema(description = "Locked lesson stable id → prerequisite stable ids not done yet; open lessons are absent",
                example = "{\"les_final\": [\"les_jwt\", \"les_feign\"]}")
        Map<String, List<String>> locks
) {
}
