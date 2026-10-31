package com.cinx.user.profile.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateEffectsRequest(
        @NotNull(message = "enabled must not be null")
        @Schema(example = "false", description = "Whether decorative animations (the owl, celebrations) play")
        Boolean enabled
) {
}
