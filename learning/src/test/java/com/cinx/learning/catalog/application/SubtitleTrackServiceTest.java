package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.command.UpdateSubtitleContentCommand;
import com.cinx.learning.catalog.application.mapper.SubtitleTrackMapperImpl;
import com.cinx.learning.catalog.application.query.SubtitleTrackView;
import com.cinx.learning.catalog.domain.SubtitleFormat;
import com.cinx.learning.catalog.domain.SubtitleSource;
import com.cinx.learning.catalog.domain.SubtitleStatus;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.shared.domain.LessonType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubtitleTrackServiceTest {
    private static final String AI_VTT = """
            WEBVTT

            00:00:00.000 --> 00:00:02.000
            Xin chào các bạn

            00:00:02.000 --> 00:00:04.000
            Hôm nay ta học vòng lặp

            00:00:04.000 --> 00:00:06.000
            Vòng lặp for trước

            00:00:06.000 --> 00:00:08.000
            Rồi tới while
            """;

    @Mock
    private SubtitleTrackRepository tracks;
    @Mock
    private VideoLessonRepository videos;
    @Mock
    private ObjectStorage storage;
    @Mock
    private ILessonService lessonService;
    @Mock
    private TranscriptChanges transcriptChanges;
    @Mock
    private ICourseAccessService courseAccess;

    private SubtitleTrackService service;

    @BeforeEach
    void setUp() {
        service = new SubtitleTrackService(tracks, videos, new SubtitleTrackMapperImpl(), new SubtitleFileProcessor(), storage,
                lessonService, new ObjectMapper(), transcriptChanges, courseAccess,
                Clock.fixed(Instant.parse("2026-10-13T09:30:00Z"), ZoneOffset.UTC));
    }

    @Test
    void aLearnerSeesOnlyApprovedTracks() {
        doReturn(Optional.of(new VideoLesson())).when(videos).findByLessonId("l1");
        doReturn(List.of(track("ready", SubtitleSource.MANUAL, SubtitleStatus.READY), track("waiting", SubtitleSource.AI_GENERATED, SubtitleStatus.REVIEW_REQUIRED)))
                .when(tracks).findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc("l1");
        doReturn(false).when(courseAccess).canManageCourse("learner-1", "c1");

        List<SubtitleTrackView> result = service.getSubtitlesByLessonId("learner-1", "c1", "l1");

        assertThat(result).extracting(SubtitleTrackView::id).containsExactly("ready");
        verify(lessonService).ensureCanReadLessonContent("learner-1", "c1", "l1", LessonType.VIDEO);
    }

    @Test
    void someoneWhoManagesTheCourseSeesTracksWaitingForReview() {
        doReturn(Optional.of(new VideoLesson())).when(videos).findByLessonId("l1");
        doReturn(List.of(track("ready", SubtitleSource.MANUAL, SubtitleStatus.READY), track("waiting", SubtitleSource.AI_GENERATED, SubtitleStatus.REVIEW_REQUIRED)))
                .when(tracks).findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc("l1");
        doReturn(true).when(courseAccess).canManageCourse("instructor-1", "c1");

        assertThat(service.getSubtitlesByLessonId("instructor-1", "c1", "l1")).hasSize(2);
    }

    @Test
    void aTrackWaitingForReviewDoesNotExistForALearner() {
        doReturn(Optional.of(track("waiting", SubtitleSource.AI_GENERATED, SubtitleStatus.REVIEW_REQUIRED))).when(tracks).findByIdAndVideoLessonLessonId("waiting", "l1");
        doReturn(false).when(courseAccess).canManageCourse("learner-1", "c1");

        assertThatThrownBy(() -> service.getSubtitleContent("learner-1", "c1", "l1", "waiting")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.getSubtitleWordConfidence("learner-1", "c1", "l1", "waiting")).isInstanceOf(NotFoundException.class);
        verify(storage, never()).readTextObject(anyString());
    }

    @Test
    void savingAnAiTrackKeepsItWaitingWithItsWordScores() {
        videoExists();
        SubtitleTrack waiting = track("waiting", SubtitleSource.AI_GENERATED, SubtitleStatus.REVIEW_REQUIRED);
        waiting.setWordConfidenceFileKey("courses/subtitles/ai/l1/default.words.json");
        doReturn(Optional.of(waiting)).when(tracks).findByIdAndVideoLessonLessonId("waiting", "l1");
        doAnswer(invocation -> invocation.getArgument(0)).when(tracks).save(any());

        SubtitleTrackView saved = service.updateSubtitleContent("instructor-1", "c1", "l1", "waiting", new UpdateSubtitleContentCommand(AI_VTT, null));

        assertThat(saved.status()).isEqualTo(SubtitleStatus.REVIEW_REQUIRED);
        assertThat(saved.source()).isEqualTo(SubtitleSource.AI_EDITED);
        assertThat(saved.wordConfidenceFileKey()).isEqualTo("courses/subtitles/ai/l1/default.words.json");
        verify(lessonService).ensureLessonInstructor("instructor-1", "c1", "l1", LessonType.VIDEO);
        verify(transcriptChanges, never()).trackChanged(anyString());
    }

    @Test
    void savingATrackLearnersSeeUpdatesTheTranscript() {
        videoExists();
        doReturn(Optional.of(track("ready", SubtitleSource.MANUAL, SubtitleStatus.READY))).when(tracks).findByIdAndVideoLessonLessonId("ready", "l1");
        doAnswer(invocation -> invocation.getArgument(0)).when(tracks).save(any());

        SubtitleTrackView saved = service.updateSubtitleContent("instructor-1", "c1", "l1", "ready", new UpdateSubtitleContentCommand(AI_VTT, null));

        assertThat(saved.status()).isEqualTo(SubtitleStatus.READY);
        assertThat(saved.source()).isEqualTo(SubtitleSource.MANUAL);
        verify(transcriptChanges).trackChanged("l1");
    }

    @Test
    void approvingRecordsTheReviewerAndTheShareOfCuesChanged() {
        videoExists();
        SubtitleTrack waiting = track("waiting", SubtitleSource.AI_EDITED, SubtitleStatus.REVIEW_REQUIRED);
        waiting.setAiOriginalFileKey("courses/subtitles/ai/l1/default.vtt");
        waiting.setFileKey("courses/subtitles/l1/vi/edited.vtt");
        doReturn(Optional.of(waiting)).when(tracks).findByIdAndVideoLessonLessonId("waiting", "l1");
        doReturn(AI_VTT).when(storage).readTextObject("courses/subtitles/ai/l1/default.vtt");
        doReturn(AI_VTT.replace("Rồi tới while", "Rồi tới vòng while")).when(storage).readTextObject("courses/subtitles/l1/vi/edited.vtt");
        doAnswer(invocation -> invocation.getArgument(0)).when(tracks).save(any());

        SubtitleTrackView approved = service.approveSubtitle("instructor-1", "c1", "l1", "waiting");

        assertThat(approved.status()).isEqualTo(SubtitleStatus.READY);
        assertThat(approved.reviewedBy()).isEqualTo("instructor-1");
        assertThat(approved.reviewedAt()).isEqualTo(LocalDateTime.of(2026, 10, 13, 9, 30));
        assertThat(approved.editedCueShare()).isEqualByComparingTo(new BigDecimal("0.25"));
        verify(lessonService).ensureLessonInstructor("instructor-1", "c1", "l1", LessonType.VIDEO);
        verify(transcriptChanges).trackChanged("l1");
    }

    @Test
    void approvingAnUntouchedTrackCountsNoEdits() {
        videoExists();
        SubtitleTrack waiting = track("waiting", SubtitleSource.AI_GENERATED, SubtitleStatus.REVIEW_REQUIRED);
        waiting.setAiOriginalFileKey("courses/subtitles/ai/l1/default.vtt");
        waiting.setFileKey("courses/subtitles/ai/l1/default.vtt");
        doReturn(Optional.of(waiting)).when(tracks).findByIdAndVideoLessonLessonId("waiting", "l1");
        doAnswer(invocation -> invocation.getArgument(0)).when(tracks).save(any());

        assertThat(service.approveSubtitle("instructor-1", "c1", "l1", "waiting").editedCueShare()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(storage, never()).readTextObject(anyString());
    }

    @Test
    void aMeasurementThatFailsDoesNotBlockApproval() {
        videoExists();
        SubtitleTrack waiting = track("waiting", SubtitleSource.AI_EDITED, SubtitleStatus.REVIEW_REQUIRED);
        waiting.setAiOriginalFileKey("courses/subtitles/ai/l1/default.vtt");
        waiting.setFileKey("courses/subtitles/l1/vi/edited.vtt");
        doReturn(Optional.of(waiting)).when(tracks).findByIdAndVideoLessonLessonId("waiting", "l1");
        doThrow(new IllegalStateException("storage down")).when(storage).readTextObject("courses/subtitles/ai/l1/default.vtt");
        doAnswer(invocation -> invocation.getArgument(0)).when(tracks).save(any());

        SubtitleTrackView approved = service.approveSubtitle("instructor-1", "c1", "l1", "waiting");

        assertThat(approved.status()).isEqualTo(SubtitleStatus.READY);
        assertThat(approved.editedCueShare()).isNull();
    }

    @Test
    void onlyATrackWaitingForReviewCanBeApproved() {
        videoExists();
        doReturn(Optional.of(track("ready", SubtitleSource.MANUAL, SubtitleStatus.READY))).when(tracks).findByIdAndVideoLessonLessonId("ready", "l1");

        assertThatThrownBy(() -> service.approveSubtitle("instructor-1", "c1", "l1", "ready")).isInstanceOf(BadRequestException.class);
        verify(tracks, never()).save(any());
        verify(transcriptChanges, never()).trackChanged(anyString());
    }

    private void videoExists() {
        doReturn(Optional.of(new VideoLesson())).when(videos).findByLessonId("l1");
    }

    private static SubtitleTrack track(String id, SubtitleSource source, SubtitleStatus status) {
        SubtitleTrack track = new SubtitleTrack();
        track.setId(id);
        track.setLanguageCode("vi");
        track.setDisplayName("Tiếng Việt");
        track.setFileKey("courses/subtitles/l1/vi/" + id + ".vtt");
        track.setFileUrl("https://cdn/" + id + ".vtt");
        track.setFileName(id + ".vtt");
        track.setFileType("text/vtt");
        track.setFileSize(100L);
        track.setFormat(SubtitleFormat.VTT);
        track.setSource(source);
        track.setStatus(status);
        track.setIsDefault(true);
        track.setVideoLessonId("l1");
        return track;
    }
}
