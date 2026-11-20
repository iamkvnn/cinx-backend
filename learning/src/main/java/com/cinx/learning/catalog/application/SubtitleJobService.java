package com.cinx.learning.catalog.application;

import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.domain.SubtitleFormat;
import com.cinx.learning.catalog.domain.SubtitleJobStatus;
import com.cinx.learning.catalog.domain.SubtitleJobType;
import com.cinx.learning.catalog.domain.SubtitleSource;
import com.cinx.learning.catalog.domain.SubtitleStatus;
import com.cinx.learning.catalog.application.command.GenerateDefaultSubtitleJobCommand;
import com.cinx.learning.catalog.application.command.SubtitleJobCompletedCommand;
import com.cinx.learning.catalog.application.command.TranslateSubtitleJobCommand;
import com.cinx.learning.catalog.application.query.SubtitleJobView;
import com.cinx.learning.catalog.application.mapper.SubtitleJobMapper;
import com.cinx.learning.catalog.application.CourseEvents;
import com.cinx.learning.catalog.application.event.SubtitleGenerateRequestedEvent;
import com.cinx.learning.catalog.application.event.SubtitleTranslateRequestedEvent;
import com.cinx.learning.catalog.domain.LessonPlacement;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SubtitleJob;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.SubtitleJobRepository;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.catalog.application.ObjectStorage;
import com.cinx.learning.notificationprojection.application.LearningNotifications;
import com.cinx.learning.notificationprojection.application.event.SubtitleJobFinishedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleJobService implements ISubtitleJobService {
    private static final String AI_SUBTITLE_STORAGE_PREFIX = "courses/subtitles/ai/";
    private static final String NORMALIZED_CONTENT_TYPE = "text/vtt";
    private static final String AUTO_LANGUAGE_CODE = "auto";
    // the worker sends summary numbers only; anything larger is not a report
    static final int MAX_QUALITY_REPORT_CHARS = 8192;
    private static final String SYSTEM_AUDITOR = "SYSTEM";

    private final SubtitleJobRepository subtitleJobRepository;
    private final SubtitleTrackRepository subtitleTrackRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final SubtitleJobMapper subtitleJobMapper;
    private final WhisperLanguageRegistry whisperLanguageRegistry;
    private final CourseEvents courseEventProducer;
    private final ILessonService lessonService;
    private final ObjectStorage s3Service;
    private final LessonRepository lessonRepository;
    private final LearningNotifications notifications;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional
    public SubtitleJobView createDefaultSubtitleJob(String currentUserId, String courseId, String lessonId, GenerateDefaultSubtitleJobCommand request) {
        VideoLesson videoLesson = ensureInstructor(currentUserId, courseId, lessonId);
        if (subtitleTrackRepository.countByVideoLessonLessonId(lessonId) > 0) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Default subtitle can only be generated when the video has no subtitles");
        }
        String languageCode = normalizeOptionalWhisperLanguage(request.languageCode());
        if (!AUTO_LANGUAGE_CODE.equals(languageCode)) {
            ensureNoExistingSubtitle(lessonId, languageCode);
        }
        ensureNoActiveJob(lessonId, languageCode);

        SubtitleJob job = newJob(videoLesson, SubtitleJobType.GENERATE_DEFAULT, null, null, languageCode, request.displayName());
        SubtitleJob saved = subtitleJobRepository.save(job);
        courseEventProducer.publishSubtitleGenerateRequestedEvent(new SubtitleGenerateRequestedEvent(
                saved.getId(),
                courseId,
                lessonId,
                videoLesson.getFileKey(),
                saved.getTargetLanguageCode(),
                saved.getDisplayName(),
                saved.getExpectedOutputFileKey()
        ));
        return subtitleJobMapper.toDto(saved);
    }

    @Override
    @Transactional
    public List<SubtitleJobView> createTranslationJobs(String currentUserId, String courseId, String lessonId, TranslateSubtitleJobCommand request) {
        VideoLesson videoLesson = ensureInstructor(currentUserId, courseId, lessonId);
        SubtitleTrack source = resolveSourceSubtitle(lessonId, request.sourceSubtitleId());
        if (source.getStatus() != SubtitleStatus.READY) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Source subtitle must be ready before translation");
        }

        List<String> targetLanguages = normalizeTargetLanguages(request.targetLanguageCodes());
        List<SubtitleJobView> responses = new ArrayList<>();
        for (String targetLanguage : targetLanguages) {
            if (targetLanguage.equals(source.getLanguageCode())) {
                throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Target language must differ from source language: " + targetLanguage);
            }
            ensureNoExistingSubtitle(lessonId, targetLanguage);
            ensureNoActiveJob(lessonId, targetLanguage);

            SubtitleJob job = newJob(
                    videoLesson,
                    SubtitleJobType.TRANSLATE,
                    source.getId(),
                    source.getLanguageCode(),
                    targetLanguage,
                    null
            );
            SubtitleJob saved = subtitleJobRepository.save(job);
            courseEventProducer.publishSubtitleTranslateRequestedEvent(new SubtitleTranslateRequestedEvent(
                    saved.getId(),
                    courseId,
                    lessonId,
                    videoLesson.getFileKey(),
                    source.getId(),
                    source.getLanguageCode(),
                    source.getFileKey(),
                    source.getFileUrl(),
                    saved.getTargetLanguageCode(),
                    saved.getDisplayName(),
                    saved.getExpectedOutputFileKey()
            ));
            responses.add(subtitleJobMapper.toDto(saved));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubtitleJobView> getJobsByLessonId(String currentUserId, String courseId, String lessonId) {
        ensureInstructor(currentUserId, courseId, lessonId);
        return subtitleJobRepository.findByVideoLessonLessonIdOrderByCreatedAtDesc(lessonId)
                .stream()
                .map(subtitleJobMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubtitleJobView getJobById(String currentUserId, String courseId, String lessonId, String jobId) {
        ensureInstructor(currentUserId, courseId, lessonId);
        return subtitleJobMapper.toDto(subtitleJobRepository.findByIdAndVideoLessonLessonId(jobId, lessonId)
                .orElseThrow(() -> new NotFoundException("Subtitle job not found with id: " + jobId)));
    }

    @Override
    @Transactional
    public void markProcessing(String jobId, Integer progressPercent) {
        SubtitleJob job = findJob(jobId);
        if (job.getStatus() == SubtitleJobStatus.SUCCEEDED || job.getStatus() == SubtitleJobStatus.FAILED) {
            return;
        }
        job.setStatus(SubtitleJobStatus.PROCESSING);
        job.setProgressPercent(clampProgress(progressPercent));
        subtitleJobRepository.save(job);
    }

    @Override
    @Transactional
    public void markCompleted(SubtitleJobCompletedCommand request) {
        SubtitleJob job = findJob(request.jobId());
        if (job.getStatus() == SubtitleJobStatus.SUCCEEDED && job.getOutputSubtitleId() != null) {
            return;
        }
        if (job.getStatus() == SubtitleJobStatus.FAILED) {
            return;
        }
        try {
            String outputLanguageCode = validateCompletion(job, request);
            ensureNoExistingSubtitle(job.getVideoLessonId(), outputLanguageCode);

            SubtitleTrack subtitleTrack = new SubtitleTrack();
            subtitleTrack.setLanguageCode(outputLanguageCode);
            subtitleTrack.setDisplayName(displayName(request.displayName(), displayName(job.getDisplayName(), whisperLanguageRegistry.displayName(outputLanguageCode))));
            subtitleTrack.setFileKey(request.outputFileKey());
            subtitleTrack.setFileUrl(resolveOutputFileUrl(request));
            subtitleTrack.setOriginalFileKey(null);
            subtitleTrack.setFileName(displayName(request.fileName(), job.getId() + ".vtt"));
            subtitleTrack.setFileType(NORMALIZED_CONTENT_TYPE);
            subtitleTrack.setFileSize(request.fileSize());
            subtitleTrack.setWordConfidenceFileKey(request.wordConfidenceFileKey());
            subtitleTrack.setWordConfidenceFileUrl(request.wordConfidenceFileUrl());
            subtitleTrack.setFormat(SubtitleFormat.VTT);
            subtitleTrack.setSource(job.getJobType() == SubtitleJobType.GENERATE_DEFAULT
                    ? SubtitleSource.AI_GENERATED
                    : SubtitleSource.AI_TRANSLATED);
            // learners see it, and the tutor indexes it, only once an instructor approves it
            subtitleTrack.setStatus(SubtitleStatus.REVIEW_REQUIRED);
            subtitleTrack.setIsDefault(job.getJobType() == SubtitleJobType.GENERATE_DEFAULT);
            subtitleTrack.setVideoLessonId(job.getVideoLessonId());
            subtitleTrack.setQualityReport(boundedReport(job.getId(), request.qualityReport()));
            subtitleTrack.setAiOriginalFileKey(request.outputFileKey());

            SubtitleTrack savedTrack = subtitleTrackRepository.save(subtitleTrack);
            job.setOutputSubtitleId(savedTrack.getId());
            job.setTargetLanguageCode(outputLanguageCode);
            job.setDisplayName(savedTrack.getDisplayName());
            job.setStatus(SubtitleJobStatus.SUCCEEDED);
            job.setProgressPercent(100);
            job.setErrorCode(null);
            job.setErrorMessage(null);
            subtitleJobRepository.save(job);
            notifyRequester(job, SubtitleJobFinishedEvent.REVIEW_REQUIRED, warningCount(savedTrack.getQualityReport()));
        } catch (BadRequestException | AlreadyExistException ex) {
            markFailedJob(job, "AI_SUBTITLE_COMPLETION_INVALID", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void markFailed(String jobId, String errorCode, String errorMessage) {
        SubtitleJob job = findJob(jobId);
        // a redelivered failure is a no-op, so the requester hears about it once
        if (job.getStatus() == SubtitleJobStatus.SUCCEEDED || job.getStatus() == SubtitleJobStatus.FAILED) {
            return;
        }
        markFailedJob(job, errorCode, errorMessage);
    }

    private SubtitleJob newJob(
            VideoLesson videoLesson,
            SubtitleJobType jobType,
            String sourceSubtitleId,
            String sourceLanguageCode,
            String targetLanguageCode,
            String displayName
    ) {
        String resolvedDisplayName = AUTO_LANGUAGE_CODE.equals(targetLanguageCode)
                ? displayName(displayName, "Auto-detected")
                : displayName(displayName, whisperLanguageRegistry.displayName(targetLanguageCode));
        SubtitleJob job = new SubtitleJob();
        job.setJobType(jobType);
        job.setStatus(SubtitleJobStatus.QUEUED);
        job.setSourceSubtitleId(sourceSubtitleId);
        job.setSourceLanguageCode(sourceLanguageCode);
        job.setTargetLanguageCode(targetLanguageCode);
        job.setDisplayName(resolvedDisplayName);
        job.setExpectedOutputFileKey(aiSubtitleFileKey(videoLesson.getLessonId(), jobType, targetLanguageCode));
        job.setProgressPercent(0);
        job.setVideoLessonId(videoLesson.getLessonId());
        return job;
    }

    private String aiSubtitleFileKey(String lessonId, SubtitleJobType jobType, String targetLanguageCode) {
        String fileName = jobType == SubtitleJobType.GENERATE_DEFAULT
                ? "default"
                : targetLanguageCode;
        return AI_SUBTITLE_STORAGE_PREFIX
                + lessonId + "/"
                + sanitizeStorageSegment(fileName)
                + ".vtt";
    }

    private String sanitizeStorageSegment(String value) {
        return value == null || value.isBlank()
                ? AUTO_LANGUAGE_CODE
                : value.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
    }

    private SubtitleTrack resolveSourceSubtitle(String lessonId, String sourceSubtitleId) {
        if (sourceSubtitleId != null && !sourceSubtitleId.isBlank()) {
            return subtitleTrackRepository.findByIdAndVideoLessonLessonId(sourceSubtitleId, lessonId)
                    .orElseThrow(() -> new NotFoundException("Source subtitle not found with id: " + sourceSubtitleId));
        }
        return subtitleTrackRepository.findByVideoLessonLessonIdAndIsDefaultTrue(lessonId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Default subtitle is required before translation"));
    }

    private List<String> normalizeTargetLanguages(List<String> targetLanguageCodes) {
        if (targetLanguageCodes == null || targetLanguageCodes.isEmpty()) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "At least one target language is required");
        }
        Set<String> seen = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String targetLanguageCode : targetLanguageCodes) {
            String languageCode = whisperLanguageRegistry.normalize(targetLanguageCode);
            if (!seen.add(languageCode)) {
                throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Duplicate target language: " + languageCode);
            }
            normalized.add(languageCode);
        }
        return normalized;
    }

    private String validateCompletion(SubtitleJob job, SubtitleJobCompletedCommand request) {
        if (request.outputFileKey() == null || !request.outputFileKey().equals(job.getExpectedOutputFileKey())) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "AI subtitle output file key does not match the job expectation");
        }
        String languageCode = whisperLanguageRegistry.normalize(request.languageCode());
        if (!AUTO_LANGUAGE_CODE.equals(job.getTargetLanguageCode()) && !languageCode.equals(job.getTargetLanguageCode())) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "AI subtitle output language does not match the job target");
        }
        if (request.fileSize() == null || request.fileSize() < 1) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "AI subtitle file size must be greater than 0");
        }
        String fileType = request.fileType() == null ? "" : request.fileType().trim();
        if (!fileType.isBlank() && !NORMALIZED_CONTENT_TYPE.equalsIgnoreCase(fileType)) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_UNSUPPORTED, "AI subtitle output must be text/vtt");
        }
        return languageCode;
    }

    private VideoLesson ensureInstructor(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.VIDEO);
        return ensureVideoLessonExists(lessonId);
    }

    private VideoLesson ensureVideoLessonExists(String lessonId) {
        return videoLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Video lesson not found for lessonId: " + lessonId));
    }

    private SubtitleJob findJob(String jobId) {
        return subtitleJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Subtitle job not found with id: " + jobId));
    }

    private void ensureNoExistingSubtitle(String lessonId, String languageCode) {
        subtitleTrackRepository.findByVideoLessonLessonIdAndLanguageCode(lessonId, languageCode)
                .ifPresent(existing -> {
                    throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Subtitle already exists for language: " + languageCode);
                });
    }

    private void ensureNoActiveJob(String lessonId, String languageCode) {
        if (subtitleJobRepository.existsByVideoLessonLessonIdAndTargetLanguageCodeAndStatusIn(
                lessonId,
                languageCode,
                List.of(SubtitleJobStatus.QUEUED, SubtitleJobStatus.PROCESSING)
        )) {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Subtitle AI job already exists for language: " + languageCode);
        }
    }

    private String normalizeOptionalWhisperLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return AUTO_LANGUAGE_CODE;
        }
        return whisperLanguageRegistry.normalize(languageCode);
    }

    private int clampProgress(Integer progressPercent) {
        if (progressPercent == null) {
            return 0;
        }
        return Math.max(0, Math.min(99, progressPercent));
    }

    private String displayName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String resolveOutputFileUrl(SubtitleJobCompletedCommand request) {
        if (request.outputFileUrl() != null && !request.outputFileUrl().isBlank()) {
            return request.outputFileUrl().trim();
        }
        return s3Service.publicUrl(request.outputFileKey());
    }

    private void markFailedJob(SubtitleJob job, String errorCode, String errorMessage) {
        job.setStatus(SubtitleJobStatus.FAILED);
        job.setErrorCode(displayName(errorCode, "AI_SUBTITLE_FAILED"));
        job.setErrorMessage(displayName(errorMessage, "AI subtitle job failed"));
        subtitleJobRepository.save(job);
        notifyRequester(job, SubtitleJobFinishedEvent.FAILED, 0);
    }

    private String boundedReport(String jobId, String report) {
        if (report == null || report.isBlank()) {
            return null;
        }
        if (report.length() > MAX_QUALITY_REPORT_CHARS) {
            log.warn("Dropped an oversized subtitle quality report jobId={} chars={}", jobId, report.length());
            return null;
        }
        return report;
    }

    private int warningCount(String report) {
        if (report == null) {
            return 0;
        }
        try {
            JsonNode warnings = objectMapper.readTree(report).path("warnings");
            return warnings.isArray() ? warnings.size() : 0;
        } catch (JsonProcessingException ex) {
            return 0;
        }
    }

    /** Tells the instructor who asked for the job; a job started by the system has nobody to tell. */
    private void notifyRequester(SubtitleJob job, String outcome, int warningCount) {
        String requester = job.getCreatedBy();
        if (requester == null || requester.isBlank() || SYSTEM_AUDITOR.equals(requester)) {
            return;
        }
        Optional<LessonPlacement> placement = lessonRepository.findPlacementsByStableId(job.getVideoLessonId()).stream().findFirst();
        if (placement.isEmpty()) {
            log.warn("No course holds the subtitle job's lesson; nobody notified jobId={} lessonId={}", job.getId(), job.getVideoLessonId());
            return;
        }
        String courseId = placement.get().courseId();
        notifications.publishSubtitleJobFinished(SubtitleJobFinishedEvent.builder()
                .jobId(job.getId())
                .userId(requester)
                .courseId(courseId)
                .lessonId(job.getVideoLessonId())
                .lessonTitle(placement.get().lessonTitle())
                .languageCode(job.getTargetLanguageCode())
                .displayName(job.getDisplayName())
                .outcome(outcome)
                .warningCount(warningCount)
                .actionUrl("/instructor/courses/" + courseId + "/edit?lesson=" + job.getVideoLessonId())
                .occurredAt(clock.instant())
                .build());
    }
}

