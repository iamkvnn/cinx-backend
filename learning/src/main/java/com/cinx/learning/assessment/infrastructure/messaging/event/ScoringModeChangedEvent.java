package com.cinx.learning.assessment.infrastructure.messaging.event;

import com.cinx.learning.shared.domain.ScoringMode;

public record ScoringModeChangedEvent(
        String quizLessonId,
        ScoringMode scoringMode
) {
}
