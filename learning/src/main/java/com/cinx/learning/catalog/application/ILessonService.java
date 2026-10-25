package com.cinx.learning.catalog.application;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.command.CreateLessonCommand;
import com.cinx.learning.catalog.application.command.MoveLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateLessonCommand;
import com.cinx.learning.catalog.application.query.LessonPositionView;
import com.cinx.learning.catalog.application.query.LessonView;
import com.cinx.learning.catalog.domain.Lesson;

import java.util.List;

public interface ILessonService {
    List<String> getLessonIdsByCourseId(String courseId);
    List<String> getEnrolledLessonIdsByCourseId(String courseId);
    LessonView getEnrolledLessonByCourseIdAndLessonId(String courseId, String lessonId);
    LessonView getUserReadableLessonByCourseIdAndLessonId(String currentUserId, String courseId, String lessonId);
    Lesson getForUpdate(String courseId, String sectionId, String lessonId, LessonType lessonType);
    void ensureLessonBelongsToCourse(String courseId, String lessonId, LessonType lessonType);
    void ensureCanReadLessonContent(String currentUserId, String courseId, String lessonId, LessonType lessonType);
    void ensureLessonInstructor(String currentUserId, String courseId, String lessonId, LessonType lessonType);
    LessonView createLesson(String currentUserId, String courseId, String sectionId, CreateLessonCommand request);
    LessonView updateLesson(String currentUserId, String courseId, String sectionId, String lessonId, UpdateLessonCommand request);
    LessonPositionView moveLesson(String currentUserId, String courseId, String lessonId, MoveLessonCommand request);
    void deleteLesson(String currentUserId, String courseId, String sectionId, String lessonId);
    boolean isLessonInstructor(String lessonStableId, String userId);
}

