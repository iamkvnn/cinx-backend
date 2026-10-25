package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateArticleLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateArticleLessonCommand;
import com.cinx.learning.catalog.application.query.ArticleLessonView;

public interface IArticleService {
    ArticleLessonView getArticleByLessonId(String currentUserId, String courseId, String lessonId);
    ArticleLessonView getReadableArticleByLessonId(String currentUserId, String courseId, String lessonId);
    void createArticle(String currentUserId, String courseId, String lessonId, CreateArticleLessonCommand request);
    void updateArticle(String currentUserId, String courseId, String lessonId, UpdateArticleLessonCommand request);
}

