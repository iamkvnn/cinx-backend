package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.shared.domain.QuizSessionStatus;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "QuizSession")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionJpaEntity extends BaseEntity {
    private String courseId;
    private String quizLessonId;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private QuizSessionStatus status;
    private Boolean isReviewAllowed;
    private Boolean isShowAnswersOnReview;

    @OneToMany(mappedBy = "quizSession")
    private List<QuizSessionQuestionJpaEntity> questions;

    @OneToOne(mappedBy = "quizSession")
    private QuizSessionSubmissionJpaEntity quizSessionSubmission;
}
