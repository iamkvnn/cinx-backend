package com.cinx.learning.assessment.application.query;

import com.cinx.learning.shared.domain.QuizSessionStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.assessment.domain.QuizSession;

/** What other features may read of a QuizSession; a copy, so the domain class stays private to assessment. */
@Value
@Builder
public class QuizSessionInfo {
    String id;
    Long version;
    String courseId;
    String quizLessonId;
    String userId;
    LocalDateTime startTime;
    LocalDateTime endTime;
    QuizSessionStatus status;
    Boolean isReviewAllowed;
    Boolean isShowAnswersOnReview;

    public static QuizSessionInfo of(QuizSession domain) {
        return QuizSessionInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .courseId(domain.getCourseId())
                .quizLessonId(domain.getQuizLessonId())
                .userId(domain.getUserId())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .status(domain.getStatus())
                .isReviewAllowed(domain.getIsReviewAllowed())
                .isShowAnswersOnReview(domain.getIsShowAnswersOnReview())
                .build();
    }
}
