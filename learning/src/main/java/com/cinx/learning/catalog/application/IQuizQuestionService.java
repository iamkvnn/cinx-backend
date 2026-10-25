package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateQuizQuestionCommand;
import com.cinx.learning.catalog.application.command.UpdateQuizQuestionCommand;
import com.cinx.learning.catalog.application.query.QuizQuestionView;

import java.util.List;

public interface IQuizQuestionService {
    List<QuizQuestionView> getQuestions(String currentUserId, String courseId, String lessonId);
    QuizQuestionView addQuestion(String currentUserId, String courseId, String lessonId, CreateQuizQuestionCommand request);
    List<QuizQuestionView> addQuestions(String lessonId, List<CreateQuizQuestionCommand> requests);
    QuizQuestionView updateQuestion(String currentUserId, String courseId, String lessonId, String questionId, UpdateQuizQuestionCommand request);
    void deleteQuestion(String currentUserId, String courseId, String lessonId, String questionId);
}

