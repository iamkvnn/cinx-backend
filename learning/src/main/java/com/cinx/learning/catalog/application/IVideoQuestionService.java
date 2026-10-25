package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateVideoQuestionCommand;
import com.cinx.learning.catalog.application.command.UpdateVideoQuestionCommand;
import com.cinx.learning.catalog.application.query.VideoQuestionView;

import java.util.List;

public interface IVideoQuestionService {
    List<VideoQuestionView> getQuestionsByLessonId(String currentUserId, String courseId, String lessonId);
    
    VideoQuestionView getQuestionById(String currentUserId, String courseId, String lessonId, String id);
    
    VideoQuestionView createQuestion(String currentUserId, String courseId, String lessonId, CreateVideoQuestionCommand request);
    
    VideoQuestionView updateQuestion(String currentUserId, String courseId, String lessonId, String id, UpdateVideoQuestionCommand request);
    
    void deleteQuestion(String currentUserId, String courseId, String lessonId, String id);
    
    boolean checkAnswer(String questionId, String userAnswer);
}

