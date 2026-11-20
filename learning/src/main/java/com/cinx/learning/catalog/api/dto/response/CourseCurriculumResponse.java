package com.cinx.learning.catalog.api.dto.response;

import java.util.List;

public record CourseCurriculumResponse(
        String courseId,
        List<CurriculumSectionResponse> sections
) {
}

