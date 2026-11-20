package com.cinx.learning.assessment.infrastructure.persistence;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "QuizSessionSubmission")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "quiz_session_id"))
public class QuizSessionSubmissionJpaEntity extends UserSubmissionJpaEntity {
    @Column(name = "quiz_session_id", nullable = false)
    private String quizSessionId;
    private Integer totalCorrectAnswers;
    private Double score;

    @OneToOne
    @JoinColumn(name = "quiz_session_id", insertable = false, updatable = false)
    private QuizSessionJpaEntity quizSession;
}
