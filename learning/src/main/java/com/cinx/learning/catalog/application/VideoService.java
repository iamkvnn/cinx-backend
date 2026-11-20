package com.cinx.learning.catalog.application;

import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.command.CreateVideoLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateVideoLessonCommand;
import com.cinx.learning.catalog.application.query.VideoLessonView;
import com.cinx.learning.catalog.application.mapper.VideoLessonMapper;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.catalog.domain.VideoQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService implements IVideoService {
    private final VideoLessonRepository videoLessonRepository;
    private final VideoLessonMapper videoLessonMapper;
    private final VideoQuestionRepository videoQuestionRepository;
    private final SubtitleTrackRepository subtitleTrackRepository;
    private final ILessonService lessonService;
    private final ICourseAccessService courseAccessService;

    @Value("${aws.s3.cdn-url}")
    private String cdnUrl;

    @Override
    public VideoLessonView getVideoByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.VIDEO);
        return getVideoOrThrow(lessonId, courseAccessService.canManageCourse(currentUserId, courseId));
    }

    @Override
    public VideoLessonView getReadableVideoByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.VIDEO);
        return getVideoOrThrow(lessonId, courseAccessService.canManageCourse(currentUserId, courseId));
    }

    /** Subtitle tracks waiting for review are listed only for someone who manages the course. */
    private VideoLessonView getVideoOrThrow(String lessonId, boolean includeUnreviewed) {
        List<SubtitleTrack> subtitles = subtitleTrackRepository.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(lessonId)
                .stream()
                .filter(track -> includeUnreviewed || track.isVisibleToLearners())
                .toList();
        return videoLessonRepository.findByLessonId(lessonId)
                .map(video -> videoLessonMapper.toDto(video, subtitles,
                        videoQuestionRepository.findByVideoLessonLessonIdOrderByTimestampSecondsAsc(lessonId).size()))
                .orElseThrow(() -> new NotFoundException("Video not found for lessonId: " + lessonId));
    }

    @Override
    public void createVideo(String currentUserId, String courseId, String lessonId, CreateVideoLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.VIDEO);
        videoLessonRepository.findByLessonId(lessonId).ifPresentOrElse(existing -> {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Video already exists for lessonId: " + lessonId);
        },() -> {
            var videoLesson = videoLessonMapper.toModel(request);
            videoLesson.setLessonId(lessonId);
            videoLesson.setVideoUrl(cdnUrl + "/" + request.getFileKey());
            videoLessonRepository.save(videoLesson);
        });
    }

    @Override
    public void updateVideo(String currentUserId, String courseId, String lessonId, UpdateVideoLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.VIDEO);
        videoLessonRepository.findByLessonId(lessonId).ifPresentOrElse(existing -> {
            videoLessonMapper.partialUpdate(existing, request);
            if (request.getFileKey() != null) {
                existing.setVideoUrl(cdnUrl + "/" + request.getFileKey());
            }
            videoLessonRepository.save(existing);
        },() -> {
            throw new NotFoundException("Video not found for lessonId: " + lessonId);
        });
    }
}

