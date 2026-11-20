package com.cinx.learning.catalog.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderCourseImagesRequest {
    @NotNull(message = "Image ids are required")
    private List<String> imageIds;
}
