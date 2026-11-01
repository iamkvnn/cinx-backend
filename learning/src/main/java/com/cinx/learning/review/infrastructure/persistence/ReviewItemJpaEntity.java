package com.cinx.learning.review.infrastructure.persistence;

import com.cinx.learning.review.domain.ReviewItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "ReviewItem")
@Table(name = "review_item")
@Getter
@Setter
@NoArgsConstructor
class ReviewItemJpaEntity {
    @Id
    private String id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "lesson_id", nullable = false)
    private String lessonId;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(nullable = false)
    private int box;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewItemStatus status;

    @Column(nullable = false, length = 20)
    private String algorithm;

    @Column(nullable = false, length = 30)
    private String reason;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
