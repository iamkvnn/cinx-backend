package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.event.CourseTranscriptsChanged;
import com.cinx.learning.catalog.domain.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TranscriptChangesTest {
    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseEvents courseEvents;

    private TranscriptChanges changes() {
        return new TranscriptChanges(lessonRepository, courseEvents, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aTrackOnAPublishedLessonAnnouncesEachCourseWithAFreshEventId() {
        when(lessonRepository.findPublishedCourseIdsByStableId("lesson-1")).thenReturn(List.of("course-1", "course-2"));
        ArgumentCaptor<CourseTranscriptsChanged> sent = ArgumentCaptor.forClass(CourseTranscriptsChanged.class);

        changes().trackChanged("lesson-1");
        changes().trackChanged("lesson-1");

        verify(courseEvents, times(4)).publishTranscriptsChanged(sent.capture());
        assertThat(sent.getAllValues()).extracting(CourseTranscriptsChanged::courseId)
                .containsExactly("course-1", "course-2", "course-1", "course-2");
        assertThat(sent.getAllValues()).allMatch(e -> e.reason() == CourseTranscriptsChanged.Reason.TRACK_CHANGED
                && e.occurredAt().equals(NOW));
        // the outbox drops a repeated id: the second write of the same lesson must still go out
        assertThat(sent.getAllValues()).extracting(CourseTranscriptsChanged::eventId).doesNotHaveDuplicates();
    }

    @Test
    void aTrackOnADraftOnlyLessonAnnouncesNothing() {
        when(lessonRepository.findPublishedCourseIdsByStableId("draft-lesson")).thenReturn(List.of());

        changes().trackChanged("draft-lesson");

        verify(courseEvents, never()).publishTranscriptsChanged(any());
    }
}
