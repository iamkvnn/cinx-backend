package com.cinx.learning.progress.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.assessment.application.InVideoAssessments;
import com.cinx.learning.assessment.application.command.SubmitVideoQuestionCommand;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.courseview.application.query.VideoLessonView;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.engagement.application.ILearningActivityService;
import com.cinx.learning.progress.application.command.TrackingVideoLessonCommand;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.progress.application.mapper.VideoLessonTrackingHistoryMapper;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistory;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistoryRepository;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {
    @Mock
    private VideoLessonTrackingHistoryMapper videoLessonTrackingHistoryMapper;
    @Mock
    private VideoLessonTrackingHistoryRepository videoLessonTrackingHistoryRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private ILearningActivityService learningActivityService;
    @Mock
    private ILearningProgressService learningProgressService;
    @Mock
    private IDailyGoalService dailyGoalService;
    @Mock
    private InVideoAssessments inVideoAssessments;
    @Spy
    private WatchedRangeTracker watchedRangeTracker = new WatchedRangeTracker(new ObjectMapper());

    @InjectMocks
    private VideoService videoService;

    @Test
    void getVideoLessonTrackingHistoriesHandsTheSortParameterToTheStore() {
        ArgumentCaptor<PageQuery> query = ArgumentCaptor.forClass(PageQuery.class);
        when(courseService.getVideoLessonById("course-1", "video-1"))
                .thenReturn(new ApiResponse<>(true, "ok", videoLesson(100, false, 0)));
        when(videoLessonTrackingHistoryRepository.findByVideoLessonId(eq("video-1"), query.capture()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));

        videoService.getVideoLessonTrackingHistories("course-1", "video-1", 1, 10, "{\"lastTrackingTime\":\"DESC\"}");

        assertThat(query.getValue().sort()).isEqualTo("{\"lastTrackingTime\":\"DESC\"}");
        assertThat(query.getValue().size()).isEqualTo(10);
    }

    @Test
    void firstVideoTrackingCreditsInitialWatchedRange() {
        VideoLessonView lesson = videoLesson(13, false, 0);
        AtomicReference<VideoLessonTrackingHistory> savedHistory = new AtomicReference<>();
        when(courseService.getVideoLessonById("course-1", "video-1"))
                .thenReturn(new ApiResponse<>(true, "ok", lesson));
        when(videoLessonTrackingHistoryRepository.findForUpdateByUserIdAndVideoLessonId("user-1", "video-1"))
                .thenAnswer(invocation -> Optional.ofNullable(savedHistory.get()));
        when(videoLessonTrackingHistoryRepository.save(any(VideoLessonTrackingHistory.class)))
                .thenAnswer(invocation -> {
                    VideoLessonTrackingHistory history = invocation.getArgument(0);
                    savedHistory.set(history);
                    return history;
                });
        when(learningProgressService.updateLearningItemProgress(
                "user-1",
                "video-1",
                new UpdateLearningItemCommand(true, true, 10.0)))
                .thenReturn(new LearningItemProgressUpdateResult(true, true, false, false));

        videoService.trackVideoProgress(
                "course-1",
                "video-1",
                "user-1",
                new TrackingVideoLessonCommand(13));

        assertEquals("[[0,13]]", savedHistory.get().getWatchedRanges());
        verify(learningProgressService).updateLearningItemProgress(
                "user-1",
                "video-1",
                new UpdateLearningItemCommand(true, true, 10.0));
        verify(dailyGoalService).recordProgress("user-1", DailyGoalType.VIDEOS_COMPLETED, 1);
    }

    @Test
    void videoDoesNotCompleteAtSeventyNinePercent() {
        VideoLessonView lesson = videoLesson(100, true, 1);
        VideoLessonTrackingHistory history = trackingHistory("[[0,79]]");
        when(courseService.getVideoLessonById("course-1", "video-1"))
                .thenReturn(new ApiResponse<>(true, "ok", lesson));
        when(courseService.checkVideoQuestionAnswer("assessment-1", "answer"))
                .thenReturn(new ApiResponse<>(true, "ok", true));
        when(videoLessonTrackingHistoryRepository.findForUpdateByUserIdAndVideoLessonId("user-1", "video-1"))
                .thenReturn(Optional.of(history));
        when(inVideoAssessments.answeredCount("user-1", "video-1")).thenReturn(1L);

        videoService.submitVideoQuestionAnswer(
                "course-1",
                "video-1",
                "user-1",
                new SubmitVideoQuestionCommand("assessment-1", "answer"));

        verify(inVideoAssessments).recordAnswer("user-1", "video-1", "assessment-1", "answer");
        verify(learningProgressService, never()).updateLearningItemProgress(any(), any(), any());
        verify(dailyGoalService, never()).recordProgress("user-1", DailyGoalType.VIDEOS_COMPLETED, 1);
    }

    @Test
    void videoCompletesAtEightyPercentWithAllQuestionsCorrect() {
        VideoLessonView lesson = videoLesson(100, true, 1);
        VideoLessonTrackingHistory history = trackingHistory("[[0,80]]");
        when(courseService.getVideoLessonById("course-1", "video-1"))
                .thenReturn(new ApiResponse<>(true, "ok", lesson));
        when(courseService.checkVideoQuestionAnswer("assessment-1", "answer"))
                .thenReturn(new ApiResponse<>(true, "ok", true));
        when(videoLessonTrackingHistoryRepository.findForUpdateByUserIdAndVideoLessonId("user-1", "video-1"))
                .thenReturn(Optional.of(history));
        when(inVideoAssessments.answeredCount("user-1", "video-1")).thenReturn(1L);
        when(learningProgressService.updateLearningItemProgress(
                "user-1",
                "video-1",
                new UpdateLearningItemCommand(true, true, 10.0)))
                .thenReturn(new LearningItemProgressUpdateResult(true, true, false, false));

        videoService.submitVideoQuestionAnswer(
                "course-1",
                "video-1",
                "user-1",
                new SubmitVideoQuestionCommand("assessment-1", "answer"));

        verify(dailyGoalService).recordProgress("user-1", DailyGoalType.VIDEOS_COMPLETED, 1);
    }

    @Test
    void anIncorrectAnswerIsNotRecorded() {
        when(courseService.getVideoLessonById("course-1", "video-1"))
                .thenReturn(new ApiResponse<>(true, "ok", videoLesson(100, true, 1)));
        when(courseService.checkVideoQuestionAnswer("assessment-1", "wrong"))
                .thenReturn(new ApiResponse<>(true, "ok", false));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> videoService.submitVideoQuestionAnswer(
                        "course-1", "video-1", "user-1", new SubmitVideoQuestionCommand("assessment-1", "wrong")))
                .isInstanceOf(com.cinx.common.exception.BadRequestException.class);

        verify(inVideoAssessments, never()).recordAnswer(any(), any(), any(), any());
    }

    @Test
    void repeatedVideoCompletionDoesNotIncrementVideoGoalAgain() {
        VideoLessonView lesson = videoLesson(100, true, 1);
        VideoLessonTrackingHistory history = trackingHistory("[[0,80]]");
        when(courseService.getVideoLessonById("course-1", "video-1"))
                .thenReturn(new ApiResponse<>(true, "ok", lesson));
        when(courseService.checkVideoQuestionAnswer("assessment-1", "answer"))
                .thenReturn(new ApiResponse<>(true, "ok", true));
        when(videoLessonTrackingHistoryRepository.findForUpdateByUserIdAndVideoLessonId("user-1", "video-1"))
                .thenReturn(Optional.of(history));
        when(inVideoAssessments.answeredCount("user-1", "video-1")).thenReturn(1L);
        when(learningProgressService.updateLearningItemProgress(
                "user-1",
                "video-1",
                new UpdateLearningItemCommand(true, true, 10.0)))
                .thenReturn(new LearningItemProgressUpdateResult(false, false, false, false));

        videoService.submitVideoQuestionAnswer(
                "course-1",
                "video-1",
                "user-1",
                new SubmitVideoQuestionCommand("assessment-1", "answer"));

        verify(dailyGoalService, never()).recordProgress("user-1", DailyGoalType.VIDEOS_COMPLETED, 1);
    }

    private VideoLessonView videoLesson(Integer duration, Boolean hasQuestions, Integer questionCount) {
        return new VideoLessonView(
                "https://video.example.test/video.mp4",
                "video.mp4",
                "video/mp4",
                100L,
                duration,
                "READY",
                hasQuestions,
                questionCount,
                false,
                0,
                null);
    }

    private VideoLessonTrackingHistory trackingHistory(String watchedRanges) {
        return VideoLessonTrackingHistory.builder()
                .userId("user-1")
                .videoLessonId("video-1")
                .currentPosition(80)
                .watchedRanges(watchedRanges)
                .lastTrackingTime(LocalDateTime.now())
                .build();
    }
}
