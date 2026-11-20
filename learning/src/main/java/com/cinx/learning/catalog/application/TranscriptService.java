package com.cinx.learning.catalog.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.query.CourseTranscriptsView;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SubtitleStatus;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.LessonType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TranscriptService implements ITranscriptService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final SubtitleTrackRepository subtitleTrackRepository;
    private final ObjectStorage objectStorage;

    @Override
    @Transactional(readOnly = true)
    public CourseTranscriptsView publishedTranscripts(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        // an archived or draft course answers empty, which tells the indexer to drop what it holds
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            return new CourseTranscriptsView(courseId, false, List.of());
        }
        List<CourseTranscriptsView.Lesson> lessons = lessonRepository.findPublishedByCourse(courseId).stream()
                .filter(lesson -> lesson.getLessonType() == LessonType.VIDEO)
                .map(lesson -> new CourseTranscriptsView.Lesson(lesson.getStableId(), lesson.getTitle(), readyTracks(lesson.getStableId())))
                .filter(lesson -> !lesson.tracks().isEmpty())
                .toList();
        return new CourseTranscriptsView(courseId, true, lessons);
    }

    private List<CourseTranscriptsView.Track> readyTracks(String lessonStableId) {
        return subtitleTrackRepository.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(lessonStableId).stream()
                .filter(track -> track.getStatus() == SubtitleStatus.READY)
                .map(this::toTrack)
                .toList();
    }

    private CourseTranscriptsView.Track toTrack(SubtitleTrack track) {
        String content = objectStorage.readTextObject(track.getFileKey());
        return new CourseTranscriptsView.Track(track.getId(), track.getLanguageCode(), Boolean.TRUE.equals(track.getIsDefault()),
                content, sha256(content), track.getUpdatedAt());
    }

    static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
