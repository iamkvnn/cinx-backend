package com.cinx.learning.catalog.application.command;

import com.cinx.learning.shared.domain.LessonType;

import java.util.List;

public record CreateLessonCommand (
     String title,
     Long duration,
     LessonType lessonType,
     Boolean isPreview,
     List<String> prerequisiteIds,
     Boolean aiAssisted
) {
}

