package com.cinx.learning.catalog.application.mapper;

import com.cinx.learning.catalog.application.query.QuizLessonView;
import com.cinx.learning.catalog.application.query.QuizQuestionView;
import com.cinx.learning.catalog.application.query.VideoLessonView;
import com.cinx.learning.catalog.domain.QuizLesson;
import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.VideoLesson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LessonViewMappersTest {
    private VideoLessonMapperImpl videoMapper;
    private QuizMapperImpl quizMapper;

    @BeforeEach
    void setUp() {
        videoMapper = new VideoLessonMapperImpl();
        ReflectionTestUtils.setField(videoMapper, "subtitleTrackMapper", new SubtitleTrackMapperImpl());
        quizMapper = new QuizMapperImpl();
    }

    @Test
    void aVideoViewCountsTheQuestionsAndSubtitlesTheCallerLoaded() {
        VideoLesson video = VideoLesson.builder().lessonId("video-1").videoUrl("https://cdn/v.mp4").duration(120).status("READY").build();
        SubtitleTrack english = SubtitleTrack.builder().id("t1").languageCode("en").isDefault(true).build();
        SubtitleTrack vietnamese = SubtitleTrack.builder().id("t2").languageCode("vi").isDefault(false).build();

        VideoLessonView view = videoMapper.toDto(video, List.of(english, vietnamese), 3);

        assertThat(view.videoUrl()).isEqualTo("https://cdn/v.mp4");
        assertThat(view.duration()).isEqualTo(120);
        assertThat(view.hasQuestions()).isTrue();
        assertThat(view.questionCount()).isEqualTo(3);
        assertThat(view.hasSubtitles()).isTrue();
        assertThat(view.subtitleCount()).isEqualTo(2);
        assertThat(view.subtitles()).extracting(subtitle -> subtitle.languageCode()).containsExactly("en", "vi");
    }

    @Test
    void aVideoWithoutQuestionsOrSubtitlesSaysSo() {
        VideoLessonView view = videoMapper.toDto(VideoLesson.builder().lessonId("video-1").build(), List.of(), 0);

        assertThat(view.hasQuestions()).isFalse();
        assertThat(view.questionCount()).isZero();
        assertThat(view.hasSubtitles()).isFalse();
        assertThat(view.subtitleCount()).isZero();
    }

    @Test
    void aQuizViewCarriesTheQuestionsTheCallerLoaded() {
        QuizLesson quiz = QuizLesson.builder().lessonId("quiz-1").maxAttempt(3).numberOfQuestionPerQuizSession(2).build();
        QuizQuestionView question = new QuizQuestionView("q-1", "2+2?", QuizQuestionType.SINGLE_CHOICE, ScoringMethod.ALL_OR_NOTHING,
                false, List.of(), null, null, false);

        QuizLessonView view = quizMapper.toDto(quiz, List.of(question));

        assertThat(view.lessonId()).isEqualTo("quiz-1");
        assertThat(view.maxAttempt()).isEqualTo(3);
        assertThat(view.questions()).containsExactly(question);
    }
}
