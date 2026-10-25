package com.cinx.learning.catalog.application.event;

import com.cinx.learning.catalog.domain.ScoringMode;
import com.cinx.learning.catalog.application.query.QuizQuestionView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSyncEvent {

    private String quizLessonId;
    private String changeReason;
    private ScoringMode scoringMode;
    private List<QuizQuestionView> questions;
}

