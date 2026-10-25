package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateVideoLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateVideoLessonCommand;
import com.cinx.learning.catalog.application.query.VideoLessonView;

public interface IVideoService {
        VideoLessonView getVideoByLessonId(String currentUserId, String courseId, String lessonId);
        VideoLessonView getReadableVideoByLessonId(String currentUserId, String courseId, String lessonId);
        void createVideo(String currentUserId, String courseId, String lessonId, CreateVideoLessonCommand request);
        void updateVideo(String currentUserId, String courseId, String lessonId, UpdateVideoLessonCommand request);
}

