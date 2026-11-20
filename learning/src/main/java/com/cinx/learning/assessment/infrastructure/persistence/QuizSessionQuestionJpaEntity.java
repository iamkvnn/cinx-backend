package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "QuizSessionQuestion")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionQuestionJpaEntity extends BaseEntity {
    private String quizSessionId;
    private String questionId;
    @Enumerated(EnumType.STRING)
    private QuizQuestionType questionType;
    private Integer questionOrder;
    @Column(length = 2000)
    private String questionText;
    private String correctAnswer;
    private String userAnswer;
    @Enumerated(EnumType.STRING)
    private ScoringMethod scoringMethod;
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String optionsSnapshot;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aiAssisted = false;

    @ManyToOne
    @JoinColumn(name = "quizSessionId", insertable = false, updatable = false)
    private QuizSessionJpaEntity quizSession;
}
