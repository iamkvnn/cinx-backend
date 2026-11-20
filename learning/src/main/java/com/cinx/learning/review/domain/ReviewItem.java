package com.cinx.learning.review.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One official quiz question a learner is reviewing, with its Leitner box and next day. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ReviewItem {
    public static final String REASON_QUIZ = "QUIZ_MISSED";

    private final String id;
    private final String userId;
    private final String courseId;
    private final String lessonId;
    private final String questionId;
    private final int box;
    /** A day in Asia/Ho_Chi_Minh. */
    private final LocalDate dueOn;
    private final ReviewItemStatus status;
    private final String algorithm;
    private final String reason;
    private final LocalDateTime lastReviewedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /** A question an official quiz scored below full marks: first box, tomorrow. */
    public static ReviewItem missedInQuiz(String id, String userId, String courseId, String lessonId, String questionId,
                                          LocalDate today, LocalDateTime now) {
        return new ReviewItem(id, userId, courseId, lessonId, questionId, LeitnerSchedule.FIRST_BOX,
                LeitnerSchedule.dueOn(LeitnerSchedule.FIRST_BOX, today), ReviewItemStatus.ACTIVE, LeitnerSchedule.ALGORITHM,
                REASON_QUIZ, null, now, now);
    }

    /** Missed again in an official quiz: back to the first box, whatever box or status it was in. */
    public ReviewItem missedAgain(LocalDate today, LocalDateTime now) {
        return toBuilder().box(LeitnerSchedule.FIRST_BOX).dueOn(LeitnerSchedule.dueOn(LeitnerSchedule.FIRST_BOX, today))
                .status(ReviewItemStatus.ACTIVE).reason(REASON_QUIZ).algorithm(LeitnerSchedule.ALGORITHM).updatedAt(now).build();
    }

    /** A review answer: fully right moves up a box (graduating from the last), anything else starts over. */
    public ReviewItem answered(boolean correct, LocalDate today, LocalDateTime now) {
        if (!correct) {
            return toBuilder().box(LeitnerSchedule.FIRST_BOX).dueOn(LeitnerSchedule.dueOn(LeitnerSchedule.FIRST_BOX, today))
                    .lastReviewedAt(now).updatedAt(now).build();
        }
        if (box >= LeitnerSchedule.LAST_BOX) {
            return toBuilder().status(ReviewItemStatus.GRADUATED).lastReviewedAt(now).updatedAt(now).build();
        }
        int next = box + 1;
        return toBuilder().box(next).dueOn(LeitnerSchedule.dueOn(next, today)).lastReviewedAt(now).updatedAt(now).build();
    }

    public ReviewItem retired(LocalDateTime now) {
        return toBuilder().status(ReviewItemStatus.RETIRED).updatedAt(now).build();
    }

    public boolean dueBy(LocalDate today) {
        return status == ReviewItemStatus.ACTIVE && !dueOn.isAfter(today);
    }
}
