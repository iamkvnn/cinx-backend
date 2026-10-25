package com.cinx.learning.catalog.application.query;

import java.util.List;

public record CourseCurriculumView(
        String courseId,
        List<CurriculumSectionView> sections
) {
}

