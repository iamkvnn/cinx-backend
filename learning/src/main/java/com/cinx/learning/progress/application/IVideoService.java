package com.cinx.learning.progress.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.assessment.application.command.SubmitVideoQuestionCommand;
import com.cinx.learning.progress.application.command.TrackingVideoLessonCommand;
import com.cinx.learning.assessment.application.query.InVideoAssessmentSubmissionView;
import com.cinx.learning.progress.application.query.VideoLessonTrackingHistoryView;

import java.util.List;

public interface IVideoService {
    PageResult<VideoLessonTrackingHistoryView> getVideoLessonTrackingHistories(String courseId, String lessonId, int page, int size, String sort);
    VideoLessonTrackingHistoryView getVideoLessonTrackingHistory(String courseId, String userId, String lessonId);
    void trackVideoProgress(String courseId, String lessonId, String userId, TrackingVideoLessonCommand request);
    void submitVideoQuestionAnswer(String courseId, String lessonId, String userId, SubmitVideoQuestionCommand request);
    List<InVideoAssessmentSubmissionView> getVideoQuestionSubmissions(String courseId, String userId, String lessonId);
}
