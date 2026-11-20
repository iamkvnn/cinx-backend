package com.cinx.learning.assessment.domain;

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
public class QuizScoreHistory extends Model {
    private String quizSessionId;
    private Double oldScore;
    private Double newScore;
    private String reason;
    private LocalDateTime gradedAt;
}
