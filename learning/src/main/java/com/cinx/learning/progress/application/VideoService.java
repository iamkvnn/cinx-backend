package com.cinx.learning.progress.application;

import com.cinx.learning.assessment.application.InVideoAssessments;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.assessment.application.command.SubmitVideoQuestionCommand;
import com.cinx.learning.progress.application.command.TrackingVideoLessonCommand;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.assessment.application.query.InVideoAssessmentSubmissionView;
import com.cinx.learning.courseview.application.query.VideoLessonView;
import com.cinx.learning.progress.application.query.VideoLessonTrackingHistoryView;
import com.cinx.learning.progress.application.mapper.VideoLessonTrackingHistoryMapper;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistory;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistoryRepository;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.engagement.application.ILearningActivityService;
import com.cinx.learning.engagement.application.IDailyGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService implements IVideoService {
    private static final int MAX_CREDITED_HEARTBEAT_SECONDS = 300;
    private static final double COMPLETION_WATCH_THRESHOLD = 0.80;

    private final VideoLessonTrackingHistoryMapper videoLessonTrackingHistoryMapper;
    private final VideoLessonTrackingHistoryRepository videoLessonTrackingHistoryRepository;
    private final CourseService courseService;
    private final ILearningActivityService learningActivityService;
    private final ILearningProgressService learningProgressService;
    private final IDailyGoalService dailyGoalService;
    private final InVideoAssessments inVideoAssessments;
    private final WatchedRangeTracker watchedRangeTracker;

    @Override
    public PageResult<VideoLessonTrackingHistoryView> getVideoLessonTrackingHistories(String courseId, String lessonId, int page, int size, String sort) {
        validatePageRequest(page, size);
        getVideoLesson(courseId, lessonId);
        return videoLessonTrackingHistoryRepository.findByVideoLessonId(lessonId, new PageQuery(page, size, sort))
                .map(videoLessonTrackingHistoryMapper::toDto);
    }

    @Override
    public VideoLessonTrackingHistoryView getVideoLessonTrackingHistory(String courseId, String userId, String lessonId) {
        getVideoLesson(courseId, lessonId);
        return videoLessonTrackingHistoryRepository.findByUserIdAndVideoLessonId(userId, lessonId)
                .map(videoLessonTrackingHistoryMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Tracking history not found for userId: " + userId + " and videoLessonId: " + lessonId));
    }

    @Override
    @Transactional
    public void trackVideoProgress(String courseId, String lessonId, String userId, TrackingVideoLessonCommand request) {
        VideoLessonView videoLessonResponse = getVideoLesson(courseId, lessonId);
        if (request.currentPosition() == null || request.currentPosition() < 0) {
            throw new BadRequestException(ErrorCode.VIDEO_POSITION_INVALID, "Current position must be a non-negative integer.");
        }
        Integer duration = videoLessonResponse != null ? videoLessonResponse.duration() : null;
        int currentPosition = clampPosition(request.currentPosition(), duration);
        LocalDateTime now = LocalDateTime.now();
        VideoLessonTrackingHistory trackingHistory = videoLessonTrackingHistoryRepository
                .findForUpdateByUserIdAndVideoLessonId(userId, lessonId)
                .orElse(null);
        boolean newTrackingHistory = trackingHistory == null;
        int previousPosition = trackingHistory != null && trackingHistory.getCurrentPosition() != null
                ? trackingHistory.getCurrentPosition()
                : 0;
        LocalDateTime previousTrackingTime = trackingHistory != null ? trackingHistory.getLastTrackingTime() : null;
        int creditedSeconds = newTrackingHistory
                ? currentPosition
                : creditedWatchedSeconds(previousPosition, currentPosition, previousTrackingTime, now);

        if (trackingHistory == null) {
            trackingHistory = VideoLessonTrackingHistory.builder()
                    .userId(userId)
                    .videoLessonId(lessonId)
                    .currentPosition(currentPosition)
                    .lastTrackingTime(now)
                    .build();
        } else if (currentPosition >= previousPosition) {
            trackingHistory.setCurrentPosition(currentPosition);
            trackingHistory.setLastTrackingTime(now);
        }

        if (creditedSeconds > 0) {
            trackingHistory.setWatchedRanges(watchedRangeTracker.merge(
                    trackingHistory.getWatchedRanges(),
                    previousPosition,
                    previousPosition + creditedSeconds,
                    duration
            ));
        }

        videoLessonTrackingHistoryRepository.save(trackingHistory);
        recordVideoActivity(userId, courseId, creditedSeconds);
        
        if (videoLessonResponse != null) {
            checkAndMarkVideoCompletion(userId, lessonId, videoLessonResponse);
        }
    }

    private void recordVideoActivity(String userId, String courseId, Integer activeSeconds) {
        if (activeSeconds == null || activeSeconds <= 0) {
            return;
        }
        learningActivityService.recordActivity(userId, courseId, activeSeconds);
    }

    @Override
    @Transactional
    public void submitVideoQuestionAnswer(String courseId, String lessonId, String userId, SubmitVideoQuestionCommand request) {
        VideoLessonView videoLessonResponse = getVideoLesson(courseId, lessonId);
        ApiResponse<Boolean> checkResult = courseService.checkVideoQuestionAnswer(
                request.videoAssessmentId(),
                request.userAnswer()
        );
        if (checkResult == null || !Boolean.TRUE.equals(checkResult.data())) {
            throw new BadRequestException(ErrorCode.VIDEO_QUESTION_ANSWER_INCORRECT, "Incorrect answer. Please try again.");
        }

        inVideoAssessments.recordAnswer(userId, lessonId, request.videoAssessmentId(), request.userAnswer());
        
        if (videoLessonResponse != null) {
            checkAndMarkVideoCompletion(userId, lessonId, videoLessonResponse);
        }
    }

    private void checkAndMarkVideoCompletion(String userId, String videoLessonId, VideoLessonView videoLessonResponse) {
        VideoLessonTrackingHistory trackingHistory = videoLessonTrackingHistoryRepository
                .findForUpdateByUserIdAndVideoLessonId(userId, videoLessonId)
                .orElse(null);
                
        double progress = 0.0;
        if (trackingHistory != null && videoLessonResponse.duration() != null && videoLessonResponse.duration() > 0) {
            log.info("Calculating progress for userId {} and videoLessonId {}: watchedRanges={}, duration={}",
                    userId, videoLessonId, trackingHistory.getWatchedRanges(), videoLessonResponse.duration());
            progress = (double) watchedRangeTracker.watchedSeconds(trackingHistory.getWatchedRanges()) / videoLessonResponse.duration();
        }

        log.info("progress for userId {} and videoLessonId {}: {}", userId, videoLessonId, progress);

        boolean watchedEnough = progress >= COMPLETION_WATCH_THRESHOLD;
        boolean questionsCompleted = true;
        
        if (Boolean.TRUE.equals(videoLessonResponse.hasQuestions()) && videoLessonResponse.questionCount() != null && videoLessonResponse.questionCount() > 0) {
            long answeredCount = inVideoAssessments.answeredCount(userId, videoLessonId);
            questionsCompleted = answeredCount >= videoLessonResponse.questionCount();
        }

        if (watchedEnough && questionsCompleted) {
            LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                    userId,
                    videoLessonId,
                    new UpdateLearningItemCommand(true, true, 10.0));
            if (result.completedTransition()) {
                dailyGoalService.recordProgress(userId, DailyGoalType.VIDEOS_COMPLETED, 1);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InVideoAssessmentSubmissionView> getVideoQuestionSubmissions(String courseId, String userId, String lessonId) {
        getVideoLesson(courseId, lessonId);
        return inVideoAssessments.submissions(userId, lessonId);
    }

    private VideoLessonView getVideoLesson(String courseId, String lessonId) {
        return courseService.getVideoLessonById(courseId, lessonId).data();
    }

    private int clampPosition(Integer position, Integer duration) {
        if (duration != null && duration > 0) {
            return Math.min(position, duration);
        }
        return position;
    }

    private int creditedWatchedSeconds(int previousPosition, int currentPosition, LocalDateTime previousTrackingTime, LocalDateTime now) {
        int positionDelta = currentPosition - previousPosition;
        if (positionDelta <= 0) {
            return 0;
        }
        if (previousTrackingTime == null) {
            return 0;
        }
        long elapsedCap = Math.min(Math.max(Duration.between(previousTrackingTime, now).getSeconds(), 0), MAX_CREDITED_HEARTBEAT_SECONDS);
        return (int) Math.min(positionDelta, elapsedCap);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1) {
            throw new BadRequestException(ErrorCode.INVALID_PAGINATION, "page must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new BadRequestException(ErrorCode.INVALID_PAGINATION, "size must be greater than or equal to 1");
        }
    }
}
