package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateQuizLessonCommand;
import com.cinx.learning.catalog.application.command.SyncQuizCommand;
import com.cinx.learning.catalog.application.command.UpdateQuizLessonCommand;
import com.cinx.learning.catalog.application.query.QuizLessonView;
import com.cinx.learning.catalog.domain.QuizLesson;

public interface IQuizService {
    QuizLesson getOrThrow(String lessonId);
    QuizLessonView getQuizByLessonId(String currentUserId, String courseId, String lessonId);
    QuizLessonView getReadableQuizByLessonId(String currentUserId, String courseId, String lessonId);
    void createQuiz(String currentUserId, String courseId, String lessonId, CreateQuizLessonCommand request);
    void updateQuiz(String currentUserId, String courseId, String lessonId, UpdateQuizLessonCommand request);
    void syncQuiz(String currentUserId, String courseId, String lessonId, SyncQuizCommand request);
}

