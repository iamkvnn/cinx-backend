package com.cinx.learning.catalog.application.query;

import java.util.List;

public record CurriculumSectionView(
        String id,
        String title,
        String description,
        Long duration,
        Integer orderIndex,
        List<LessonView> lessons
) {
}

