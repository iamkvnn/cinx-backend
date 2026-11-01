package com.cinx.learning.review.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One review answer, kept for audit and for replaying the same request. */
@Getter
@Builder
@AllArgsConstructor
public class ReviewAttempt {
    private final String id;
    private final String reviewItemId;
    private final String userId;
    private final String questionId;
    private final String userAnswer;
    private final BigDecimal score;
    private final boolean correct;
    private final int boxBefore;
    private final int boxAfter;
    /** Null once the question graduates. */
    private final LocalDate dueOnAfter;
    private final LocalDateTime answeredAt;
    private final String idempotencyKey;
    private final String requestFingerprint;
}
