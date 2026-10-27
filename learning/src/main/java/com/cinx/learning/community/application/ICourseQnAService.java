package com.cinx.learning.community.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.community.application.command.CreateAnswerCommand;
import com.cinx.learning.community.application.command.CreateQnAReportCommand;
import com.cinx.learning.community.application.command.CreateQuestionCommand;
import com.cinx.learning.community.application.command.UpdateAnswerCommand;
import com.cinx.learning.community.application.command.UpdateQuestionCommand;
import com.cinx.learning.community.application.query.AnswerView;
import com.cinx.learning.community.application.query.QuestionView;

import java.util.List;

public interface ICourseQnAService {
    QuestionView createQuestion(String userId, CreateQuestionCommand request);
    PageResult<QuestionView> getQuestionsByCourse(String courseId, String lessonId, String currentUserId, int page, int size, String query, String sort, boolean unanswered);
    QuestionView getQuestionById(String questionId, String currentUserId);
    /** Questions already asked in the course that read like this text, so a learner can read before posting. */
    List<QuestionView> findSimilarQuestions(String courseId, String lessonId, String text, String currentUserId);
    QuestionView updateQuestion(String userId, String questionId, UpdateQuestionCommand request);
    void deleteQuestion(String userId, String questionId);
    void deleteQuestionByAdmin(String questionId);
    void upvoteQuestion(String userId, String questionId);
    void reportQuestion(String userId, String questionId, CreateQnAReportCommand request);

    AnswerView createAnswer(String userId, String questionId, CreateAnswerCommand request);
    PageResult<AnswerView> getAnswersForQuestion(String questionId, String currentUserId, int page, int size, String sort);
    PageResult<AnswerView> getReplies(String parentAnswerId, String currentUserId, int page, int size, String sort);
    AnswerView updateAnswer(String userId, String answerId, UpdateAnswerCommand request);
    void deleteAnswer(String userId, String answerId);
    void deleteAnswerByAdmin(String answerId);
    void upvoteAnswer(String userId, String answerId);
    void markAnswerHelpful(String userId, String answerId);
    void unmarkAnswerHelpful(String userId, String answerId);
    void reportAnswer(String userId, String answerId, CreateQnAReportCommand request);
}
