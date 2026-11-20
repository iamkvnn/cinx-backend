package com.cinx.learning.learningpath.application;

import com.cinx.learning.learningpath.application.command.LearningPathItemsUpdateCommand;
import com.cinx.learning.learningpath.application.command.LearningPathCommand;
import com.cinx.learning.learningpath.application.query.LearningPathView;
import java.util.List;

public interface ILearningPathService {
    /**
     * Create the user's learning path.  With an idempotency key, repeating the key with the same
     * request returns the path created the first time; with another request it is rejected.
     */
    LearningPathView createLearningPath(String userId, String idempotencyKey, LearningPathCommand request);

    default LearningPathView createLearningPath(String userId, LearningPathCommand request) {
        return createLearningPath(userId, null, request);
    }
    /**
     * Edit the caller's active or pending path in place: the items are replaced, progress and status are recomputed,
     * and a course new to the path that the learner does not own goes to the cart.
     */
    LearningPathView replaceItems(String userId, String id, LearningPathItemsUpdateCommand request);

    LearningPathView getActiveLearningPath(String userId);
    List<LearningPathView> getLearningPaths(String userId);
    LearningPathView getLearningPath(String userId, String id);
    void dropActiveLearningPath(String userId);
    void activatePendingPathForCourse(String userId, String courseId);
    void updatePathProgress(String userId, String lessonId);

    /**
     * Re-evaluate learning-path item unlock state for a single user after
     * prerequisite or lesson-order changes in a course.  Preserves completed
     * items; only marks not-yet-started items as unlocked or re-locked.
     */
    void refreshPrerequisiteUnlocks(String userId, String courseId);
}
