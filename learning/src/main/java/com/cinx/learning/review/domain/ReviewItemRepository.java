package com.cinx.learning.review.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where review items are stored. */
public interface ReviewItemRepository {
    Optional<ReviewItem> findByIdAndUserId(String id, String userId);

    List<ReviewItem> findByUserIdAndQuestionIdIn(String userId, Collection<String> questionIds);

    /** ACTIVE items due by the day: the most overdue first, then the lowest box. */
    List<ReviewItem> findDue(String userId, LocalDate day, int limit);

    long countDue(String userId, LocalDate day);

    List<String> findUserIdsWithDue(LocalDate day);

    ReviewItem save(ReviewItem item);

    List<ReviewItem> saveAll(List<ReviewItem> items);
}
