package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.application.query.CourseTranscriptsView;

import java.time.LocalDateTime;
import java.util.List;

/** GET /internal/courses/{courseId}/transcripts: a published course's subtitle text, for the assistant's index. */
public record CourseTranscriptsResponse(String courseId, boolean published, List<Lesson> lessons) {

    public record Lesson(String lessonId, String title, List<Track> tracks) {
    }

    public record Track(String trackId, String languageCode, boolean isDefault, String content, String sha256,
                        LocalDateTime updatedAt) {
    }

    public static CourseTranscriptsResponse from(CourseTranscriptsView view) {
        return new CourseTranscriptsResponse(view.courseId(), view.published(), view.lessons().stream()
                .map(lesson -> new Lesson(lesson.lessonId(), lesson.title(), lesson.tracks().stream()
                        .map(t -> new Track(t.trackId(), t.languageCode(), t.isDefault(), t.content(), t.sha256(), t.updatedAt()))
                        .toList()))
                .toList());
    }
}
