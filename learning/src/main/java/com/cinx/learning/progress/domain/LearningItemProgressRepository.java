package com.cinx.learning.progress.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where LearningItemProgress rows are stored. */
public interface LearningItemProgressRepository {
    /** Learners who completed each item of one course (rows are pre-created at enrolment, so only is_completed counts). */
    List<ItemCount> completedPerItem(String courseId);

    /** Who passed these items of one course. */
    List<PassedRow> passedIn(String courseId, java.util.Collection<String> itemIds);

    List<LearningItemProgress> findAllByUserIdAndCourseId(String userId, String courseId);

    Optional<LearningItemProgress> findByItemIdAndUserId(String itemId, String userId);

    Optional<LearningItemProgress> findByItemIdAndUserIdForUpdate(String itemId, String userId);

    List<LearningItemProgress> findAllByUserIdAndItemIdIn(String userId, List<String> itemIds);

    void deleteAll(Iterable<LearningItemProgress> entities);

    LearningItemProgress save(LearningItemProgress entity);

    List<LearningItemProgress> saveAll(Iterable<LearningItemProgress> entities);

    interface ItemCount {
            String getItemId();
            long getTotal();
        }

    interface PassedRow {
            String getItemId();
            String getUserId();
        }
}
