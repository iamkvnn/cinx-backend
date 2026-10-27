package com.cinx.learning.community.application;

import com.cinx.learning.community.application.command.BatchAnswerCommand;
import com.cinx.learning.community.application.query.InstructorQnAResponses;

import java.util.List;

public interface IInstructorQnAService {
    List<InstructorQnAResponses.Question> unansweredInLesson(String currentUserId, String lessonId);

    List<InstructorQnAResponses.PastAnswer> myAnswersInLesson(String currentUserId, String lessonId);

    InstructorQnAResponses.BatchResult answerTogether(String currentUserId, BatchAnswerCommand request);
}
