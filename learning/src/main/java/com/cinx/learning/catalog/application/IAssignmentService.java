package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateAssignmentLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateAssignmentLessonCommand;
import com.cinx.learning.catalog.application.query.AssignmentLessonView;

public interface IAssignmentService {
    AssignmentLessonView getAssignmentByLessonId(String currentUserId, String courseId, String lessonId);
    void createAssignment(String currentUserId, String courseId, String lessonId, CreateAssignmentLessonCommand request);
    void updateAssignment(String currentUserId, String courseId, String lessonId, UpdateAssignmentLessonCommand request);
}

