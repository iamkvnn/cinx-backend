package com.cinx.learning.catalog.application.event;

import com.cinx.learning.catalog.domain.ScoringMode;

public record ScoringModeChangedEvent(
        String quizLessonId,
        ScoringMode scoringMode
) {
}

