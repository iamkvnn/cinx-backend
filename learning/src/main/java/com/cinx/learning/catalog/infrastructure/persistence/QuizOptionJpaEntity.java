package com.cinx.learning.catalog.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity(name = "QuizOption")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizOptionJpaEntity extends BaseEntity {
    private String optionText;
    private Boolean isCorrect;
    private Integer optionOrder;
    private String matchText;

    @ManyToOne
    private QuizQuestionJpaEntity quizQuestion;
}

