package com.cinx.learning.catalog.api.dto.response;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentLessonResponse (
        @Schema(example = "Please submit your completed project via ZIP file.")
        String description,
        List<AssignmentAttachmentResponse> attachments
){
}

