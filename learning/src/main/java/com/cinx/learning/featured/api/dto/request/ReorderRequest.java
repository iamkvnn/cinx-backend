package com.cinx.learning.featured.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** The complete list of item ids in their new order; every existing item must appear exactly once. */
public record ReorderRequest(
        @NotEmpty
        @Schema(description = "All item ids, in the new order")
        List<String> ids
) {
}
