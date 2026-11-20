package com.cinx.learning.review.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "ReviewAttempt")
@Table(name = "review_attempt")
@Getter
@Setter
@NoArgsConstructor
class ReviewAttemptJpaEntity {
    @Id
    private String id;

    @Column(name = "review_item_id", nullable = false)
    private String reviewItemId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Lob
    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "box_before", nullable = false)
    private int boxBefore;

    @Column(name = "box_after", nullable = false)
    private int boxAfter;

    @Column(name = "due_on_after")
    private LocalDate dueOnAfter;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;
}
