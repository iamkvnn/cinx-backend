package com.cinx.learning.review.infrastructure.persistence;

import com.cinx.learning.review.domain.ReviewAttempt;
import com.cinx.learning.review.domain.ReviewAttemptRepository;
import com.cinx.learning.review.domain.ReviewItem;
import com.cinx.learning.review.domain.ReviewItemRepository;
import com.cinx.learning.review.domain.ReviewItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ReviewItemRepositoryAdapter implements ReviewItemRepository {
    private final ReviewItemJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewItem> findByIdAndUserId(String id, String userId) {
        return jpa.findByIdAndUserId(id, userId).map(ReviewItemRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> findByUserIdAndQuestionIdIn(String userId, Collection<String> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByUserIdAndQuestionIdIn(userId, questionIds).stream().map(ReviewItemRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> findDue(String userId, LocalDate day, int limit) {
        return jpa.findDue(userId, ReviewItemStatus.ACTIVE, day, PageRequest.of(0, limit)).stream()
                .map(ReviewItemRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDue(String userId, LocalDate day) {
        return jpa.countByUserIdAndStatusAndDueOnLessThanEqual(userId, ReviewItemStatus.ACTIVE, day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUserIdsWithDue(LocalDate day) {
        return jpa.findUserIdsWithDue(ReviewItemStatus.ACTIVE, day);
    }

    @Override
    @Transactional
    public ReviewItem save(ReviewItem item) {
        return toDomain(jpa.save(write(item)));
    }

    @Override
    @Transactional
    public List<ReviewItem> saveAll(List<ReviewItem> items) {
        return jpa.saveAll(items.stream().map(this::write).toList()).stream().map(ReviewItemRepositoryAdapter::toDomain).toList();
    }

    /** Edits the stored row in place, so its lock version stays the store's. */
    private ReviewItemJpaEntity write(ReviewItem item) {
        ReviewItemJpaEntity row = jpa.findById(item.getId()).orElseGet(ReviewItemJpaEntity::new);
        row.setId(item.getId());
        row.setUserId(item.getUserId());
        row.setCourseId(item.getCourseId());
        row.setLessonId(item.getLessonId());
        row.setQuestionId(item.getQuestionId());
        row.setBox(item.getBox());
        row.setDueOn(item.getDueOn());
        row.setStatus(item.getStatus());
        row.setAlgorithm(item.getAlgorithm());
        row.setReason(item.getReason());
        row.setLastReviewedAt(item.getLastReviewedAt());
        row.setCreatedAt(item.getCreatedAt());
        row.setUpdatedAt(item.getUpdatedAt());
        return row;
    }

    private static ReviewItem toDomain(ReviewItemJpaEntity row) {
        return ReviewItem.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .courseId(row.getCourseId())
                .lessonId(row.getLessonId())
                .questionId(row.getQuestionId())
                .box(row.getBox())
                .dueOn(row.getDueOn())
                .status(row.getStatus())
                .algorithm(row.getAlgorithm())
                .reason(row.getReason())
                .lastReviewedAt(row.getLastReviewedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class ReviewAttemptRepositoryAdapter implements ReviewAttemptRepository {
    private final ReviewAttemptJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewAttempt> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey) {
        return jpa.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(ReviewAttemptRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public ReviewAttempt insert(ReviewAttempt attempt) {
        ReviewAttemptJpaEntity row = new ReviewAttemptJpaEntity();
        row.setId(attempt.getId());
        row.setReviewItemId(attempt.getReviewItemId());
        row.setUserId(attempt.getUserId());
        row.setQuestionId(attempt.getQuestionId());
        row.setUserAnswer(attempt.getUserAnswer());
        row.setScore(attempt.getScore());
        row.setCorrect(attempt.isCorrect());
        row.setBoxBefore(attempt.getBoxBefore());
        row.setBoxAfter(attempt.getBoxAfter());
        row.setDueOnAfter(attempt.getDueOnAfter());
        row.setAnsweredAt(attempt.getAnsweredAt());
        row.setIdempotencyKey(attempt.getIdempotencyKey());
        row.setRequestFingerprint(attempt.getRequestFingerprint());
        // flushed now, so a concurrent duplicate key fails inside the caller's transaction
        return toDomain(jpa.saveAndFlush(row));
    }

    private static ReviewAttempt toDomain(ReviewAttemptJpaEntity row) {
        return ReviewAttempt.builder()
                .id(row.getId())
                .reviewItemId(row.getReviewItemId())
                .userId(row.getUserId())
                .questionId(row.getQuestionId())
                .userAnswer(row.getUserAnswer())
                .score(row.getScore())
                .correct(row.isCorrect())
                .boxBefore(row.getBoxBefore())
                .boxAfter(row.getBoxAfter())
                .dueOnAfter(row.getDueOnAfter())
                .answeredAt(row.getAnsweredAt())
                .idempotencyKey(row.getIdempotencyKey())
                .requestFingerprint(row.getRequestFingerprint())
                .build();
    }
}
