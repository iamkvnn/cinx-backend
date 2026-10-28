package com.cinx.learning.catalog.application;

import com.cinx.common.dto.PresignedUrlResponse;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.domain.SubtitleFormat;
import com.cinx.learning.catalog.domain.SubtitleSource;
import com.cinx.learning.catalog.domain.SubtitleStatus;
import com.cinx.learning.catalog.application.command.CreateSubtitleTrackCommand;
import com.cinx.learning.catalog.application.command.UpdateSubtitleContentCommand;
import com.cinx.learning.catalog.application.command.UpdateSubtitleTrackCommand;
import com.cinx.learning.catalog.application.query.SubtitleContentView;
import com.cinx.learning.catalog.application.query.SubtitleTrackView;
import com.cinx.learning.catalog.application.query.SubtitleWordConfidenceView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cinx.learning.catalog.application.mapper.SubtitleTrackMapper;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.catalog.application.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleTrackService implements ISubtitleTrackService {
    private static final String SUBTITLE_UPLOAD_PREFIX = "courses/subtitles/uploads/";
    private static final String SUBTITLE_STORAGE_PREFIX = "courses/subtitles/";

    private final SubtitleTrackRepository subtitleTrackRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final SubtitleTrackMapper subtitleTrackMapper;
    private final SubtitleFileProcessor subtitleFileProcessor;
    private final ObjectStorage s3Service;
    private final ILessonService lessonService;
    private final ObjectMapper objectMapper;
    private final TranscriptChanges transcriptChanges;
    private final ICourseAccessService courseAccessService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<SubtitleTrackView> getSubtitlesByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.VIDEO);
        ensureVideoLessonExists(lessonId);
        boolean manager = courseAccessService.canManageCourse(currentUserId, courseId);
        return subtitleTrackRepository.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(lessonId)
                .stream()
                .filter(track -> manager || track.isVisibleToLearners())
                .map(subtitleTrackMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PresignedUrlResponse getSubtitlePresignedUrl(
            String currentUserId,
            String courseId,
            String lessonId,
            String fileName,
            String contentType,
            String languageCode
    ) {
        ensureInstructor(currentUserId, courseId, lessonId);
        String normalizedLanguage = subtitleFileProcessor.normalizeLanguageCode(languageCode);
        subtitleFileProcessor.detectFormat(fileName, contentType);

        String fileKey = SUBTITLE_UPLOAD_PREFIX
                + lessonId + "/"
                + normalizedLanguage + "/"
                + UUID.randomUUID() + "-"
                + sanitizeFileName(fileName);
        return PresignedUrlResponse.builder()
                .fileKey(fileKey)
                .presignedUrl(s3Service.generatePresignedUrl(fileKey, contentType))
                .build();
    }

    @Override
    @Transactional
    public SubtitleTrackView createSubtitle(String currentUserId, String courseId, String lessonId, CreateSubtitleTrackCommand request) {
        VideoLesson videoLesson = ensureInstructor(currentUserId, courseId, lessonId);
        String languageCode = subtitleFileProcessor.normalizeLanguageCode(request.languageCode());
        subtitleTrackRepository.findByVideoLessonLessonIdAndLanguageCode(lessonId, languageCode)
                .ifPresent(existing -> {
                    throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Subtitle already exists for language: " + languageCode);
                });

        SubtitleTrack subtitleTrack = new SubtitleTrack();
        applyUploadedFile(subtitleTrack, lessonId, languageCode, request.fileKey(), request.fileName(), request.fileType(), request.fileSize());
        subtitleTrack.setLanguageCode(languageCode);
        subtitleTrack.setDisplayName(request.displayName().trim());
        subtitleTrack.setSource(SubtitleSource.MANUAL);
        subtitleTrack.setStatus(SubtitleStatus.READY);
        subtitleTrack.setVideoLessonId(videoLesson.getLessonId());
        subtitleTrack.setIsDefault(resolveDefaultFlag(lessonId, request.isDefault()));
        demoteOtherDefaultsIfNeeded(lessonId, subtitleTrack);

        SubtitleTrack saved = subtitleTrackRepository.save(subtitleTrack);
        transcriptChanges.trackChanged(lessonId);
        return subtitleTrackMapper.toDto(saved);
    }

    @Override
    @Transactional
    public SubtitleTrackView updateSubtitle(String currentUserId, String courseId, String lessonId, String subtitleId, UpdateSubtitleTrackCommand request) {
        ensureInstructor(currentUserId, courseId, lessonId);
        SubtitleTrack subtitleTrack = subtitleTrackRepository.findByIdAndVideoLessonLessonId(subtitleId, lessonId)
                .orElseThrow(() -> new NotFoundException("Subtitle not found with id: " + subtitleId));

        subtitleTrackMapper.partialUpdate(subtitleTrack, request);
        if (request.displayName() != null && request.displayName().isBlank()) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Subtitle display name must not be blank");
        }
        if (hasNewFile(request)) {
            if (request.fileKey() == null || request.fileName() == null || request.fileType() == null || request.fileSize() == null) {
                throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "fileKey, fileName, fileType and fileSize are required when replacing subtitle file");
            }
            applyUploadedFile(
                    subtitleTrack,
                    lessonId,
                    subtitleTrack.getLanguageCode(),
                    request.fileKey(),
                    request.fileName(),
                    request.fileType(),
                    request.fileSize()
            );
            subtitleTrack.setStatus(SubtitleStatus.READY);
            subtitleTrack.setSource(SubtitleSource.MANUAL);
            subtitleTrack.setWordConfidenceFileKey(null);
            subtitleTrack.setWordConfidenceFileUrl(null);
        }
        if (request.isDefault() != null) {
            subtitleTrack.setIsDefault(request.isDefault());
        }
        if (Boolean.TRUE.equals(subtitleTrack.getIsDefault())) {
            demoteOtherDefaultsIfNeeded(lessonId, subtitleTrack);
        } else {
            promoteAnotherDefaultIfNeeded(lessonId, subtitleTrack);
        }
        if (subtitleTrackRepository.countByVideoLessonLessonId(lessonId) == 1) {
            subtitleTrack.setIsDefault(true);
        }

        SubtitleTrack saved = subtitleTrackRepository.save(subtitleTrack);
        // a new name or default flag leaves the words alone; only a new file changes the transcript
        if (hasNewFile(request)) {
            transcriptChanges.trackChanged(lessonId);
        }
        return subtitleTrackMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubtitleContentView getSubtitleContent(String currentUserId, String courseId, String lessonId, String subtitleId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.VIDEO);
        SubtitleTrack subtitleTrack = findReadableSubtitle(currentUserId, courseId, lessonId, subtitleId);
        return new SubtitleContentView(subtitleTrack.getId(), s3Service.readTextObject(subtitleTrack.getFileKey()));
    }

    @Override
    @Transactional
    public SubtitleTrackView updateSubtitleContent(String currentUserId, String courseId, String lessonId, String subtitleId, UpdateSubtitleContentCommand request) {
        ensureInstructor(currentUserId, courseId, lessonId);
        SubtitleTrack subtitleTrack = findSubtitle(lessonId, subtitleId);
        String normalized = subtitleFileProcessor.normalizeToWebVtt(SubtitleFormat.VTT, request.content());
        String editedFileKey = SUBTITLE_STORAGE_PREFIX
                + lessonId + "/"
                + subtitleTrack.getLanguageCode() + "/"
                + UUID.randomUUID() + "-edited.vtt";

        s3Service.uploadTextFile(editedFileKey, normalized, SubtitleFileProcessor.NORMALIZED_CONTENT_TYPE);
        subtitleTrack.setOriginalFileKey(subtitleTrack.getFileKey());
        subtitleTrack.setFileKey(editedFileKey);
        subtitleTrack.setFileUrl(s3Service.publicUrl(editedFileKey));
        subtitleTrack.setFileName(subtitleTrack.getId() + "-edited.vtt");
        subtitleTrack.setFileType(SubtitleFileProcessor.NORMALIZED_CONTENT_TYPE);
        subtitleTrack.setFileSize((long) normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        subtitleTrack.setFormat(SubtitleFormat.VTT);
        if (subtitleTrack.isAiOutput()) {
            // word scores hang off timestamps, so the reviewer still sees doubtful words in the cues not yet changed
            subtitleTrack.setSource(SubtitleSource.AI_EDITED);
        } else {
            subtitleTrack.setSource(SubtitleSource.MANUAL);
            subtitleTrack.setWordConfidenceFileKey(null);
            subtitleTrack.setWordConfidenceFileUrl(null);
        }
        // saving is not approving: a track waiting for review keeps waiting
        if (subtitleTrack.getStatus() != SubtitleStatus.REVIEW_REQUIRED) {
            subtitleTrack.setStatus(SubtitleStatus.READY);
        }
        if (request.displayName() != null) {
            if (request.displayName().isBlank()) {
                throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Subtitle display name must not be blank");
            }
            subtitleTrack.setDisplayName(request.displayName().trim());
        }
        SubtitleTrack saved = subtitleTrackRepository.save(subtitleTrack);
        if (saved.isVisibleToLearners()) {
            transcriptChanges.trackChanged(lessonId);
        }
        return subtitleTrackMapper.toDto(saved);
    }

    @Override
    @Transactional
    public SubtitleTrackView approveSubtitle(String currentUserId, String courseId, String lessonId, String subtitleId) {
        ensureInstructor(currentUserId, courseId, lessonId);
        SubtitleTrack subtitleTrack = findSubtitle(lessonId, subtitleId);
        if (subtitleTrack.getStatus() != SubtitleStatus.REVIEW_REQUIRED) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Only a subtitle waiting for review can be approved");
        }
        subtitleTrack.setEditedCueShare(editedCueShare(subtitleTrack));
        subtitleTrack.setStatus(SubtitleStatus.READY);
        subtitleTrack.setReviewedBy(currentUserId);
        subtitleTrack.setReviewedAt(LocalDateTime.now(clock));
        SubtitleTrack saved = subtitleTrackRepository.save(subtitleTrack);
        transcriptChanges.trackChanged(lessonId);
        return subtitleTrackMapper.toDto(saved);
    }

    /** Share of the AI cues no longer in the file unchanged; null when it cannot be measured, which never blocks approval. */
    private BigDecimal editedCueShare(SubtitleTrack subtitleTrack) {
        String aiOriginal = subtitleTrack.getAiOriginalFileKey();
        if (aiOriginal == null) {
            return null;
        }
        if (aiOriginal.equals(subtitleTrack.getFileKey())) {
            return BigDecimal.ZERO.setScale(4);
        }
        try {
            List<String> aiCues = subtitleFileProcessor.cueSignatures(s3Service.readTextObject(aiOriginal));
            if (aiCues.isEmpty()) {
                return null;
            }
            Map<String, Integer> current = new HashMap<>();
            subtitleFileProcessor.cueSignatures(s3Service.readTextObject(subtitleTrack.getFileKey()))
                    .forEach(cue -> current.merge(cue, 1, Integer::sum));
            int kept = 0;
            for (String cue : aiCues) {
                int left = current.getOrDefault(cue, 0);
                if (left > 0) {
                    kept++;
                    current.put(cue, left - 1);
                }
            }
            return BigDecimal.valueOf(aiCues.size() - kept).divide(BigDecimal.valueOf(aiCues.size()), 4, RoundingMode.HALF_UP);
        } catch (RuntimeException ex) {
            log.warn("Could not measure the reviewer's edits subtitleId={}", subtitleTrack.getId(), ex);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SubtitleWordConfidenceView getSubtitleWordConfidence(String currentUserId, String courseId, String lessonId, String subtitleId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.VIDEO);
        SubtitleTrack subtitleTrack = findReadableSubtitle(currentUserId, courseId, lessonId, subtitleId);
        String confidenceFileKey = subtitleTrack.getWordConfidenceFileKey();
        if (confidenceFileKey == null || confidenceFileKey.isBlank()) {
            throw new NotFoundException("AI word confidence file not found for subtitle: " + subtitleId);
        }
        try {
            String content = s3Service.readTextObject(confidenceFileKey);
            List<SubtitleWordConfidenceView.SubtitleWordConfidenceItem> words = objectMapper.readValue(
                    content,
                    new TypeReference<>() {}
            );
            return new SubtitleWordConfidenceView(subtitleTrack.getId(), words);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "AI word confidence file is invalid");
        } catch (RuntimeException ex) {
            throw new NotFoundException("AI word confidence file not found for subtitle: " + subtitleId);
        }
    }

    private void promoteAnotherDefaultIfNeeded(String lessonId, SubtitleTrack selectedSubtitle) {
        boolean hasAnotherDefault = subtitleTrackRepository.findByVideoLessonLessonIdAndIsDefaultTrue(lessonId)
                .stream()
                .anyMatch(track -> !Objects.equals(track.getId(), selectedSubtitle.getId()));
        if (hasAnotherDefault) {
            return;
        }
        subtitleTrackRepository.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(lessonId)
                .stream()
                .filter(track -> !Objects.equals(track.getId(), selectedSubtitle.getId()))
                .findFirst()
                .ifPresent(nextDefault -> {
                    nextDefault.setIsDefault(true);
                    subtitleTrackRepository.save(nextDefault);
                });
    }

    @Override
    @Transactional
    public void deleteSubtitle(String currentUserId, String courseId, String lessonId, String subtitleId) {
        ensureInstructor(currentUserId, courseId, lessonId);
        SubtitleTrack subtitleTrack = subtitleTrackRepository.findByIdAndVideoLessonLessonId(subtitleId, lessonId)
                .orElseThrow(() -> new NotFoundException("Subtitle not found with id: " + subtitleId));
        boolean wasDefault = Boolean.TRUE.equals(subtitleTrack.getIsDefault());
        subtitleTrackRepository.delete(subtitleTrack);
        subtitleTrackRepository.flush();

        if (wasDefault) {
            subtitleTrackRepository.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(lessonId)
                    .stream()
                    .findFirst()
                    .ifPresent(nextDefault -> {
                        nextDefault.setIsDefault(true);
                        subtitleTrackRepository.save(nextDefault);
                    });
        }
        transcriptChanges.trackChanged(lessonId);
    }

    private VideoLesson ensureVideoLessonExists(String lessonId) {
        return videoLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Video lesson not found for lessonId: " + lessonId));
    }

    private SubtitleTrack findSubtitle(String lessonId, String subtitleId) {
        return subtitleTrackRepository.findByIdAndVideoLessonLessonId(subtitleId, lessonId)
                .orElseThrow(() -> new NotFoundException("Subtitle not found with id: " + subtitleId));
    }

    /** A track waiting for review does not exist for anyone who cannot manage the course. */
    private SubtitleTrack findReadableSubtitle(String currentUserId, String courseId, String lessonId, String subtitleId) {
        SubtitleTrack subtitleTrack = findSubtitle(lessonId, subtitleId);
        if (!subtitleTrack.isVisibleToLearners() && !courseAccessService.canManageCourse(currentUserId, courseId)) {
            throw new NotFoundException("Subtitle not found with id: " + subtitleId);
        }
        return subtitleTrack;
    }

    private VideoLesson ensureInstructor(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.VIDEO);
        return ensureVideoLessonExists(lessonId);
    }

    private void applyUploadedFile(
            SubtitleTrack subtitleTrack,
            String lessonId,
            String languageCode,
            String sourceFileKey,
            String fileName,
            String fileType,
            Long fileSize
    ) {
        subtitleFileProcessor.validateFileSize(fileSize);
        SubtitleFormat sourceFormat = subtitleFileProcessor.detectFormat(fileName, fileType);
        String uploadedContent = s3Service.readTextObject(sourceFileKey);
        String webVttContent = subtitleFileProcessor.normalizeToWebVtt(sourceFormat, uploadedContent);
        String normalizedFileKey = SUBTITLE_STORAGE_PREFIX
                + lessonId + "/"
                + languageCode + "/"
                + UUID.randomUUID() + ".vtt";

        s3Service.uploadTextFile(normalizedFileKey, webVttContent, SubtitleFileProcessor.NORMALIZED_CONTENT_TYPE);

        subtitleTrack.setOriginalFileKey(sourceFileKey);
        subtitleTrack.setFileKey(normalizedFileKey);
        subtitleTrack.setFileUrl(s3Service.publicUrl(normalizedFileKey));
        subtitleTrack.setFileName(fileName.trim());
        subtitleTrack.setFileType(SubtitleFileProcessor.NORMALIZED_CONTENT_TYPE);
        subtitleTrack.setFileSize((long) webVttContent.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        subtitleTrack.setFormat(SubtitleFormat.VTT);
    }

    private boolean resolveDefaultFlag(String lessonId, Boolean requestedDefault) {
        return Boolean.TRUE.equals(requestedDefault) || subtitleTrackRepository.countByVideoLessonLessonId(lessonId) == 0;
    }

    private void demoteOtherDefaultsIfNeeded(String lessonId, SubtitleTrack selectedDefault) {
        if (!Boolean.TRUE.equals(selectedDefault.getIsDefault())) {
            return;
        }
        List<SubtitleTrack> defaults = subtitleTrackRepository.findByVideoLessonLessonIdAndIsDefaultTrue(lessonId)
                .stream()
                .filter(track -> !Objects.equals(track.getId(), selectedDefault.getId()))
                .toList();
        defaults.forEach(track -> track.setIsDefault(false));
        subtitleTrackRepository.saveAll(defaults);
    }

    private boolean hasNewFile(UpdateSubtitleTrackCommand request) {
        return request.fileKey() != null || request.fileName() != null || request.fileType() != null || request.fileSize() != null;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "Subtitle file name is required");
        }
        return fileName.trim().replaceAll("[\\\\/]+", "-");
    }
}

