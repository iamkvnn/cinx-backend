package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.SubtitleJobCompletedCommand;
import com.cinx.learning.catalog.application.mapper.SubtitleJobMapperImpl;
import com.cinx.learning.catalog.domain.LessonPlacement;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SubtitleJob;
import com.cinx.learning.catalog.domain.SubtitleJobRepository;
import com.cinx.learning.catalog.domain.SubtitleJobStatus;
import com.cinx.learning.catalog.domain.SubtitleJobType;
import com.cinx.learning.catalog.domain.SubtitleSource;
import com.cinx.learning.catalog.domain.SubtitleStatus;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.notificationprojection.application.LearningNotifications;
import com.cinx.learning.notificationprojection.application.event.SubtitleJobFinishedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SubtitleJobServiceTest {
    private static final String AI_KEY = "courses/subtitles/ai/l1/default.vtt";
    private static final String REPORT = "{\"schemaVersion\":1,\"warnings\":[\"CPS_HIGH\",\"OVERLAP\"]}";

    @Mock
    private SubtitleJobRepository jobs;
    @Mock
    private SubtitleTrackRepository tracks;
    @Mock
    private VideoLessonRepository videos;
    @Mock
    private CourseEvents courseEvents;
    @Mock
    private ILessonService lessonService;
    @Mock
    private ObjectStorage storage;
    @Mock
    private LessonRepository lessons;
    @Mock
    private LearningNotifications notifications;

    private SubtitleJobService service;

    @BeforeEach
    void setUp() {
        service = new SubtitleJobService(jobs, tracks, videos, new SubtitleJobMapperImpl(), new WhisperLanguageRegistry(),
                courseEvents, lessonService, storage, lessons, notifications, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-10-13T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void aCompletedJobWaitsForReviewAndTellsTheRequester() {
        SubtitleJob job = job(SubtitleJobStatus.PROCESSING, "instructor-1");
        doReturn(Optional.of(job)).when(jobs).findById("job-1");
        doReturn(Optional.empty()).when(tracks).findByVideoLessonLessonIdAndLanguageCode("l1", "vi");
        doAnswer(invocation -> {
            SubtitleTrack track = invocation.getArgument(0);
            track.setId("track-1");
            return track;
        }).when(tracks).save(any());
        doReturn(List.of(new LessonPlacement("c1", "Vòng lặp"))).when(lessons).findPlacementsByStableId("l1");

        service.markCompleted(completed(AI_KEY, REPORT));

        ArgumentCaptor<SubtitleTrack> track = ArgumentCaptor.forClass(SubtitleTrack.class);
        verify(tracks).save(track.capture());
        assertThat(track.getValue().getStatus()).isEqualTo(SubtitleStatus.REVIEW_REQUIRED);
        assertThat(track.getValue().getSource()).isEqualTo(SubtitleSource.AI_GENERATED);
        assertThat(track.getValue().getQualityReport()).isEqualTo(REPORT);
        assertThat(track.getValue().getAiOriginalFileKey()).isEqualTo(AI_KEY);
        assertThat(job.getStatus()).isEqualTo(SubtitleJobStatus.SUCCEEDED);

        ArgumentCaptor<SubtitleJobFinishedEvent> event = ArgumentCaptor.forClass(SubtitleJobFinishedEvent.class);
        verify(notifications).publishSubtitleJobFinished(event.capture());
        assertThat(event.getValue().getOutcome()).isEqualTo("REVIEW_REQUIRED");
        assertThat(event.getValue().getUserId()).isEqualTo("instructor-1");
        assertThat(event.getValue().getWarningCount()).isEqualTo(2);
        assertThat(event.getValue().getLessonTitle()).isEqualTo("Vòng lặp");
        assertThat(event.getValue().getActionUrl()).isEqualTo("/instructor/courses/c1/edit?lesson=l1");
        assertThat(event.getValue().getOccurredAt()).isEqualTo(Instant.parse("2026-10-13T09:00:00Z"));
        // learners and the tutor wait for the approval, so no transcript change goes out yet
        verifyNoInteractions(courseEvents);
    }

    @Test
    void aRedeliveredCompletionIsANoOp() {
        SubtitleJob job = job(SubtitleJobStatus.SUCCEEDED, "instructor-1");
        job.setOutputSubtitleId("track-1");
        doReturn(Optional.of(job)).when(jobs).findById("job-1");

        service.markCompleted(completed(AI_KEY, REPORT));

        verify(tracks, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void anOversizedReportIsDropped() {
        doReturn(Optional.of(job(SubtitleJobStatus.PROCESSING, "instructor-1"))).when(jobs).findById("job-1");
        doReturn(Optional.empty()).when(tracks).findByVideoLessonLessonIdAndLanguageCode("l1", "vi");
        doAnswer(invocation -> invocation.getArgument(0)).when(tracks).save(any());
        doReturn(List.of(new LessonPlacement("c1", "Vòng lặp"))).when(lessons).findPlacementsByStableId("l1");
        String huge = "{\"warnings\":[],\"pad\":\"" + "x".repeat(SubtitleJobService.MAX_QUALITY_REPORT_CHARS) + "\"}";

        service.markCompleted(completed(AI_KEY, huge));

        ArgumentCaptor<SubtitleTrack> track = ArgumentCaptor.forClass(SubtitleTrack.class);
        verify(tracks).save(track.capture());
        assertThat(track.getValue().getQualityReport()).isNull();
        ArgumentCaptor<SubtitleJobFinishedEvent> event = ArgumentCaptor.forClass(SubtitleJobFinishedEvent.class);
        verify(notifications).publishSubtitleJobFinished(event.capture());
        assertThat(event.getValue().getWarningCount()).isZero();
    }

    @Test
    void aCompletionThatDoesNotMatchTheJobFailsItAndSaysSo() {
        SubtitleJob job = job(SubtitleJobStatus.PROCESSING, "instructor-1");
        doReturn(Optional.of(job)).when(jobs).findById("job-1");
        doReturn(List.of(new LessonPlacement("c1", "Vòng lặp"))).when(lessons).findPlacementsByStableId("l1");

        service.markCompleted(completed("courses/subtitles/ai/other/default.vtt", REPORT));

        assertThat(job.getStatus()).isEqualTo(SubtitleJobStatus.FAILED);
        verify(tracks, never()).save(any());
        ArgumentCaptor<SubtitleJobFinishedEvent> event = ArgumentCaptor.forClass(SubtitleJobFinishedEvent.class);
        verify(notifications).publishSubtitleJobFinished(event.capture());
        assertThat(event.getValue().getOutcome()).isEqualTo("FAILED");
    }

    @Test
    void aFailedJobTellsTheRequesterOnce() {
        SubtitleJob job = job(SubtitleJobStatus.PROCESSING, "instructor-1");
        doReturn(Optional.of(job)).when(jobs).findById("job-1");
        doReturn(List.of(new LessonPlacement("c1", "Vòng lặp"))).when(lessons).findPlacementsByStableId("l1");

        service.markFailed("job-1", "AI_SUBTITLE_FAILED", "Whisper ran out of memory");

        assertThat(job.getStatus()).isEqualTo(SubtitleJobStatus.FAILED);
        ArgumentCaptor<SubtitleJobFinishedEvent> event = ArgumentCaptor.forClass(SubtitleJobFinishedEvent.class);
        verify(notifications).publishSubtitleJobFinished(event.capture());
        assertThat(event.getValue().getOutcome()).isEqualTo("FAILED");
        assertThat(event.getValue().getWarningCount()).isZero();
    }

    @Test
    void aRedeliveredFailureIsANoOp() {
        doReturn(Optional.of(job(SubtitleJobStatus.FAILED, "instructor-1"))).when(jobs).findById("job-1");

        service.markFailed("job-1", "AI_SUBTITLE_FAILED", "again");

        verify(jobs, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void aJobTheSystemStartedNotifiesNobody() {
        doReturn(Optional.of(job(SubtitleJobStatus.PROCESSING, "SYSTEM"))).when(jobs).findById("job-1");

        service.markFailed("job-1", "AI_SUBTITLE_FAILED", "failed");

        verifyNoInteractions(notifications, lessons);
    }

    private static SubtitleJob job(SubtitleJobStatus status, String createdBy) {
        SubtitleJob job = new SubtitleJob();
        job.setId("job-1");
        job.setJobType(SubtitleJobType.GENERATE_DEFAULT);
        job.setStatus(status);
        job.setTargetLanguageCode("auto");
        job.setDisplayName("Auto-detected");
        job.setExpectedOutputFileKey(AI_KEY);
        job.setVideoLessonId("l1");
        job.setProgressPercent(60);
        job.setCreatedBy(createdBy);
        return job;
    }

    private static SubtitleJobCompletedCommand completed(String outputKey, String report) {
        return new SubtitleJobCompletedCommand("job-1", outputKey, "https://cdn/" + outputKey, "job-1.vtt", "text/vtt", 120L,
                "vi", "Tiếng Việt", "courses/subtitles/ai/l1/default.words.json", "https://cdn/words.json", report);
    }
}
