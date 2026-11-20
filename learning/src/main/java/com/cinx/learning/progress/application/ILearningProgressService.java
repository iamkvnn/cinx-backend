package com.cinx.learning.progress.application;

import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.progress.application.query.LearningItemProgressView;

import java.util.List;

public interface ILearningProgressService {
    List<CourseProgressView> getCourseProgressByCourseIds(String userId, List<String> courseIds);
    CourseProgressView getCourseProgress(String userId, String courseId);
    List<LearningItemProgressView> getLearningItemProgressByCourseId(String userId, String courseId);
    List<CourseProgressView> getCourseProgressByCourseId(String courseId);
    void createCourseProgress(String userId, String courseId);
    boolean isLearningItemCompleted(String userId, String itemId);
    LearningItemProgressUpdateResult updateLearningItemProgress(String userId, String itemId, UpdateLearningItemCommand request);
    void completeArticleItem(String userId, String itemId);
    void recomputeCourseProgress(String courseId);
}
