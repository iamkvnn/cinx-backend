package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.GenerateDefaultSubtitleJobCommand;
import com.cinx.learning.catalog.application.command.SubtitleJobCompletedCommand;
import com.cinx.learning.catalog.application.command.TranslateSubtitleJobCommand;
import com.cinx.learning.catalog.application.query.SubtitleJobView;

import java.util.List;

public interface ISubtitleJobService {
    SubtitleJobView createDefaultSubtitleJob(String currentUserId, String courseId, String lessonId, GenerateDefaultSubtitleJobCommand request);

    List<SubtitleJobView> createTranslationJobs(String currentUserId, String courseId, String lessonId, TranslateSubtitleJobCommand request);

    List<SubtitleJobView> getJobsByLessonId(String currentUserId, String courseId, String lessonId);

    SubtitleJobView getJobById(String currentUserId, String courseId, String lessonId, String jobId);

    void markProcessing(String jobId, Integer progressPercent);

    void markCompleted(SubtitleJobCompletedCommand request);

    void markFailed(String jobId, String errorCode, String errorMessage);
}

