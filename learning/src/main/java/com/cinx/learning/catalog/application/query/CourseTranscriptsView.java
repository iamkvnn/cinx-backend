package com.cinx.learning.catalog.application.query;

import java.time.LocalDateTime;
import java.util.List;

/** The READY subtitle tracks of a course's published video lessons, content inline, for transcript indexing. */
public record CourseTranscriptsView(String courseId, boolean published, List<Lesson> lessons) {

    /** {@code lessonId} is the lesson's stable id, the one the player and the subtitle API use. */
    public record Lesson(String lessonId, String title, List<Track> tracks) {
    }

    /** {@code sha256} of the content lets a consumer skip a track it already indexed. */
    public record Track(String trackId, String languageCode, boolean isDefault, String content, String sha256,
                        LocalDateTime updatedAt) {
    }
}
