package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/** A rejection carries an overall reason, the reviewer's drafted notes, or both; at least one is required. */
public record RejectCourseRequest(
        @Size(max = 2000)
        @Schema(example = "Course content violates copyright policies.")
        String reason,
        @Size(max = 100)
        @Schema(description = "Draft review notes to send with the rejection")
        List<String> noteIds
) {
}
