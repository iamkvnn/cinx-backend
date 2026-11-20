package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity(name = "QuizQuestion")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionJpaEntity extends BaseEntity {
    @Column(length = 2000)
    private String questionText;

    @Enumerated(EnumType.STRING)
    private QuizQuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ScoringMethod scoringMethod = ScoringMethod.ALL_OR_NOTHING;

    @Builder.Default
    private Boolean needSync = false;

    /** Essay questions only; instructor-facing, never part of what a learner receives. */
    @Column(columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aiAssisted = false;

    @OneToMany(mappedBy = "quizQuestion")
    private List<QuizOptionJpaEntity> options;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_lesson_id")
    private QuizLessonJpaEntity quizLesson;
}

