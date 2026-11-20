package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.event.CourseTranscriptsChanged;
import com.cinx.learning.catalog.domain.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Announces that a published course's transcripts changed after a subtitle track write.
 * Tracks hang off the lesson's stable id and are shared by its draft and published copies, so a track
 * written on a lesson that only a draft holds announces nothing.
 */
@Component
@RequiredArgsConstructor
public class TranscriptChanges {
    private final LessonRepository lessonRepository;
    private final CourseEvents courseEvents;
    private final Clock clock;

    /** Call inside the write's transaction, so the event and the track change commit together. */
    public void trackChanged(String lessonStableId) {
        for (String courseId : lessonRepository.findPublishedCourseIdsByStableId(lessonStableId)) {
            courseEvents.publishTranscriptsChanged(new CourseTranscriptsChanged(
                    UUID.randomUUID().toString(), courseId, CourseTranscriptsChanged.Reason.TRACK_CHANGED, clock.instant()));
        }
    }
}
