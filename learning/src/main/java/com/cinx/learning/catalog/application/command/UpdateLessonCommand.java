package com.cinx.learning.catalog.application.command;

import java.util.List;

public record UpdateLessonCommand(
     String title,
     Long duration,
     Boolean isPreview,
     List<String> prerequisiteIds
) {
}

