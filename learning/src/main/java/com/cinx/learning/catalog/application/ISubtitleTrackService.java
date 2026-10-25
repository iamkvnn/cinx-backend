package com.cinx.learning.catalog.application;

import com.cinx.common.dto.PresignedUrlResponse;
import com.cinx.learning.catalog.application.command.CreateSubtitleTrackCommand;
import com.cinx.learning.catalog.application.command.UpdateSubtitleContentCommand;
import com.cinx.learning.catalog.application.command.UpdateSubtitleTrackCommand;
import com.cinx.learning.catalog.application.query.SubtitleContentView;
import com.cinx.learning.catalog.application.query.SubtitleTrackView;
import com.cinx.learning.catalog.application.query.SubtitleWordConfidenceView;

import java.util.List;

public interface ISubtitleTrackService {
    List<SubtitleTrackView> getSubtitlesByLessonId(String currentUserId, String courseId, String lessonId);

    PresignedUrlResponse getSubtitlePresignedUrl(String currentUserId, String courseId, String lessonId, String fileName, String contentType, String languageCode);

    SubtitleTrackView createSubtitle(String currentUserId, String courseId, String lessonId, CreateSubtitleTrackCommand request);

    SubtitleTrackView updateSubtitle(String currentUserId, String courseId, String lessonId, String subtitleId, UpdateSubtitleTrackCommand request);

    SubtitleContentView getSubtitleContent(String currentUserId, String courseId, String lessonId, String subtitleId);

    SubtitleTrackView updateSubtitleContent(String currentUserId, String courseId, String lessonId, String subtitleId, UpdateSubtitleContentCommand request);

    SubtitleWordConfidenceView getSubtitleWordConfidence(String currentUserId, String courseId, String lessonId, String subtitleId);

    /** Publishes an AI track waiting for review to learners, recording who approved it and how much they changed. */
    SubtitleTrackView approveSubtitle(String currentUserId, String courseId, String lessonId, String subtitleId);

    void deleteSubtitle(String currentUserId, String courseId, String lessonId, String subtitleId);
}

