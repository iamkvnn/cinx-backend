package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.domain.ArticleLesson;
import com.cinx.learning.catalog.domain.ArticleLessonRepository;
import com.cinx.learning.catalog.domain.AssignmentLesson;
import com.cinx.learning.catalog.domain.AssignmentLessonRepository;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.shared.domain.LessonType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseReadinessServiceTest {
    @Mock
    private ICourseDraftService courseDraftService;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private VideoLessonRepository videoLessonRepository;
    @Mock
    private ArticleLessonRepository articleLessonRepository;
    @Mock
    private AssignmentLessonRepository assignmentLessonRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @InjectMocks
    private CourseReadinessService service;

    private final List<Lesson> lessons = List.of(
            lesson("video-ready", LessonType.VIDEO),
            lesson("video-empty", LessonType.VIDEO),
            lesson("article-url", LessonType.ARTICLE),
            lesson("article-empty", LessonType.ARTICLE),
            lesson("quiz-ready", LessonType.QUIZ),
            lesson("quiz-empty", LessonType.QUIZ),
            lesson("assignment-ready", LessonType.ASSIGNMENT),
            lesson("assignment-blank", LessonType.ASSIGNMENT));

    @Test
    void aLessonHasContentWhenItsVideoFileItsArticleItsQuestionsOrItsBriefExist() {
        stubContent();

        Set<String> ready = service.lessonsWithContent(lessons);

        assertThat(ready).containsExactlyInAnyOrder("video-ready", "article-url", "quiz-ready", "assignment-ready");
    }

    @Test
    void theSameLessonsHeldAsInfoGiveTheSameAnswer() {
        stubContent();
        List<LessonInfo> infos = lessons.stream().map(LessonInfo::of).toList();

        assertThat(service.lessonsWithContentOf(infos)).isEqualTo(service.lessonsWithContent(lessons));
    }

    @Test
    void aLessonWithoutATypeNeverCounts() {
        Set<String> ready = service.lessonsWithContent(List.of(lesson("untyped", null)));

        assertThat(ready).isEmpty();
    }

    private void stubContent() {
        when(videoLessonRepository.findAllByLessonIdIn(any())).thenReturn(List.of(
                VideoLesson.builder().lessonId("video-ready").fileKey("videos/a.mp4").build(),
                VideoLesson.builder().lessonId("video-empty").fileKey(" ").build()));
        when(articleLessonRepository.findAllByLessonIdIn(any())).thenReturn(List.of(
                ArticleLesson.builder().lessonId("article-url").articleUrl("https://a.example/post").build(),
                ArticleLesson.builder().lessonId("article-empty").build()));
        when(quizQuestionRepository.countPerQuizLesson(any())).thenReturn(List.of(
                new QuizCount("quiz-ready", 3), new QuizCount("quiz-empty", 0)));
        when(assignmentLessonRepository.findAllByLessonIdIn(any())).thenReturn(List.of(
                AssignmentLesson.builder().lessonId("assignment-ready").description("Write a report").build(),
                AssignmentLesson.builder().lessonId("assignment-blank").description("  ").build()));
    }

    private static Lesson lesson(String stableId, LessonType type) {
        return Lesson.builder().stableId(stableId).lessonType(type).build();
    }

    private record QuizCount(String lessonId, long total) implements QuizQuestionRepository.QuizCount {
        @Override
        public String getLessonId() {
            return lessonId;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }
}
