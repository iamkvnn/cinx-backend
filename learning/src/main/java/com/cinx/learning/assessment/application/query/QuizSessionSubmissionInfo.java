package com.cinx.learning.assessment.application.query;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.assessment.domain.QuizSessionSubmission;

/** What other features may read of a QuizSessionSubmission; a copy, so the domain class stays private to assessment. */
@Value
@Builder
public class QuizSessionSubmissionInfo {
    String id;
    Long version;
    String userId;
    LocalDateTime submissionTime;
    String quizSessionId;
    Integer totalCorrectAnswers;
    Double score;

    public static QuizSessionSubmissionInfo of(QuizSessionSubmission domain) {
        return QuizSessionSubmissionInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .userId(domain.getUserId())
                .submissionTime(domain.getSubmissionTime())
                .quizSessionId(domain.getQuizSessionId())
                .totalCorrectAnswers(domain.getTotalCorrectAnswers())
                .score(domain.getScore())
                .build();
    }
}
