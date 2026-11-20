package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.CourseTranscriptsView;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SubtitleStatus;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.LessonType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private SubtitleTrackRepository subtitleTrackRepository;
    @Mock
    private ObjectStorage objectStorage;

    @InjectMocks
    private TranscriptService service;

    private static Course course(CourseStatus status) {
        Course course = new Course();
        course.setId("course-1");
        course.setStatus(status);
        return course;
    }

    private static SubtitleTrack track(String id, String language, SubtitleStatus status) {
        SubtitleTrack track = new SubtitleTrack();
        track.setId(id);
        track.setLanguageCode(language);
        track.setStatus(status);
        track.setFileKey("key-" + id);
        track.setIsDefault("vi".equals(language));
        return track;
    }

    @Test
    void anArchivedCourseAnswersEmptySoTheIndexDropsIt() {
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course(CourseStatus.ARCHIVED)));

        CourseTranscriptsView view = service.publishedTranscripts("course-1");

        assertThat(view.published()).isFalse();
        assertThat(view.lessons()).isEmpty();
        verify(lessonRepository, never()).findPublishedByCourse(anyString());
    }

    @Test
    void onlyReadyTracksOfPublishedVideoLessonsAreReturnedWithTheirHash() {
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course(CourseStatus.PUBLISHED)));
        Lesson video = Lesson.builder().stableId("lesson-1").title("Exception").lessonType(LessonType.VIDEO).build();
        Lesson article = Lesson.builder().stableId("lesson-2").title("Notes").lessonType(LessonType.ARTICLE).build();
        when(lessonRepository.findPublishedByCourse("course-1")).thenReturn(List.of(video, article));
        when(subtitleTrackRepository.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc("lesson-1"))
                .thenReturn(List.of(track("t-vi", "vi", SubtitleStatus.READY), track("t-en", "en", SubtitleStatus.PROCESSING)));
        when(objectStorage.readTextObject("key-t-vi")).thenReturn("WEBVTT");

        CourseTranscriptsView view = service.publishedTranscripts("course-1");

        assertThat(view.published()).isTrue();
        assertThat(view.lessons()).singleElement().satisfies(lesson -> {
            assertThat(lesson.lessonId()).isEqualTo("lesson-1");
            assertThat(lesson.tracks()).singleElement().satisfies(t -> {
                assertThat(t.trackId()).isEqualTo("t-vi");
                assertThat(t.content()).isEqualTo("WEBVTT");
                // the digest itself is pinned by sha256IsHexOfTheUtf8Bytes; here only that one is attached
                assertThat(t.sha256()).matches("[0-9a-f]{64}");
            });
        });
    }

    @Test
    void sha256IsHexOfTheUtf8Bytes() {
        // sha256("abc") from FIPS 180-2
        assertThat(TranscriptService.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
