package com.cinx.learning.assessment.domain;

import com.cinx.learning.shared.domain.QuizSessionStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession extends Model {
    private String courseId;
    private String quizLessonId;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private QuizSessionStatus status;
    private Boolean isReviewAllowed;
    private Boolean isShowAnswersOnReview;
}
