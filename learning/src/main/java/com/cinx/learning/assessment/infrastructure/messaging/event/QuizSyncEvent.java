package com.cinx.learning.assessment.infrastructure.messaging.event;

import com.cinx.learning.shared.domain.ScoringMode;
import com.cinx.learning.courseview.application.query.QuizQuestionView;
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
