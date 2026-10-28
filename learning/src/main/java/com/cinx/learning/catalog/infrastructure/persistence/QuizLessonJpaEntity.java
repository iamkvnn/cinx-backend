package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.ScoringMode;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity(name = "QuizLesson")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizLessonJpaEntity {
    @Id
    private String lessonId;
    private Integer duration;
    private Integer numberOfQuestionPerQuizSession;
    private Integer maxAttempt;

    @Builder.Default
    private Boolean isReviewAllowed = false;

    @Builder.Default
    private Boolean isShowAnswersOnReview = false;

    @Builder.Default
    private Boolean shuffleQuestions = false;

    @Builder.Default
    private Boolean shuffleOptions = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ScoringMode scoringMode = ScoringMode.HIGHEST;

    @Builder.Default
    private Boolean hasPendingSync = false;

    @OneToMany(mappedBy = "quizLesson")
    private List<QuizQuestionJpaEntity> questions;
}

