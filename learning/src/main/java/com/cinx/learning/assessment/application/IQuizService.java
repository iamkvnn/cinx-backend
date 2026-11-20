package com.cinx.learning.assessment.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.assessment.application.command.ChooseQuizAnswerCommand;
import com.cinx.learning.assessment.application.command.GradeEssayCommand;
import com.cinx.learning.assessment.application.command.SubmitQuizSessionCommand;
import com.cinx.learning.assessment.application.query.QuizQuestionAnalyticsView;
import com.cinx.learning.assessment.application.query.QuizSessionQuestionView;
import com.cinx.learning.assessment.application.query.QuizSessionView;

import java.util.List;

public interface IQuizService {
    PageResult<QuizSessionView> getQuizSessions(String userId, String lessonId, int page, int size, String sort);
    QuizSessionView getQuizSession(String id);
    PageResult<QuizSessionQuestionView> getQuizSessionQuestions(String quizSessionId, int page, int size, String sort);
    QuizSessionView createQuizSession(String courseId, String userId, String lessonId);
    void chooseQuizSessionQuestion(String quizSessionId, ChooseQuizAnswerCommand request);
    QuizSessionView submitQuizSession(String quizSessionId, SubmitQuizSessionCommand request);
    QuizSessionView gradeEssay(String sessionId, GradeEssayCommand request);

    List<QuizQuestionAnalyticsView> getQuizAnalytics(String courseId, String lessonId);
}
