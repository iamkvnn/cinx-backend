package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizOption extends Model {
    private String optionText;
    private Boolean isCorrect;
    private Integer optionOrder;
    private String matchText;
    private String quizQuestionId;
}
