package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.learning.catalog.application.command.CreateQuizOptionCommand;
import com.cinx.learning.catalog.application.command.CreateQuizQuestionCommand;
import com.cinx.learning.catalog.application.mapper.QuizQuestionMapper;
import com.cinx.learning.catalog.domain.QuizLesson;
import com.cinx.learning.catalog.domain.QuizLessonRepository;
import com.cinx.learning.catalog.domain.QuizOption;
import com.cinx.learning.catalog.domain.QuizOptionRepository;
import com.cinx.learning.catalog.domain.QuizQuestion;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizQuestionServiceTest {
    @Mock
    private QuizQuestionMapper quizQuestionMapper;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private QuizOptionRepository quizOptionRepository;
    @Mock
    private QuizLessonRepository quizLessonRepository;
    @Mock
    private ILessonService lessonService;
    @Mock
    private ICourseAccessService courseAccessService;

    @InjectMocks
    private QuizQuestionService service;

    @Test
    void theOptionsOfANewQuestionAreStoredWithTheIdTheStoreGaveTheQuestion() {
        when(quizLessonRepository.findByLessonId("quiz-1")).thenReturn(Optional.of(QuizLesson.builder().lessonId("quiz-1").build()));
        when(quizQuestionRepository.save(any(QuizQuestion.class))).thenAnswer(invocation -> {
            QuizQuestion saved = QuizQuestion.builder().id("question-9").questionText("2+2?").build();
            return saved;
        });
        when(quizOptionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.addQuestion("instructor-1", "course-1", "quiz-1", single());

        verify(lessonService).ensureLessonInstructor("instructor-1", "course-1", "quiz-1", LessonType.QUIZ);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<QuizOption>> options = ArgumentCaptor.forClass(Iterable.class);
        verify(quizOptionRepository).saveAll(options.capture());
        assertThat(options.getValue()).extracting(QuizOption::getQuizQuestionId).containsOnly("question-9");
        assertThat(options.getValue()).extracting(QuizOption::getOptionText).containsExactly("3", "4");
        assertThat(options.getValue()).extracting(QuizOption::getIsCorrect).containsExactly(false, true);
    }

    @Test
    void severalQuestionsKeepEachOptionWithItsOwnQuestion() {
        when(quizLessonRepository.findByLessonId("quiz-1")).thenReturn(Optional.of(QuizLesson.builder().lessonId("quiz-1").build()));
        // the store answers in the order it was given
        when(quizQuestionRepository.saveAll(any())).thenReturn(List.of(
                QuizQuestion.builder().id("question-1").build(),
                QuizQuestion.builder().id("question-2").build(),
                QuizQuestion.builder().id("question-3").build()));
        when(quizOptionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.addQuestions("quiz-1", List.of(single(), essay(), single()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<QuizOption>> options = ArgumentCaptor.forClass(Iterable.class);
        verify(quizOptionRepository).saveAll(options.capture());
        // two options for the first question, none for the essay, two for the third
        assertThat(options.getValue()).extracting(QuizOption::getQuizQuestionId)
                .containsExactly("question-1", "question-1", "question-3", "question-3");
    }

    @Test
    void aChoiceQuestionWithoutOptionsIsRefused() {
        when(quizLessonRepository.findByLessonId("quiz-1")).thenReturn(Optional.of(QuizLesson.builder().lessonId("quiz-1").build()));
        CreateQuizQuestionCommand noOptions = new CreateQuizQuestionCommand("?", QuizQuestionType.SINGLE_CHOICE,
                ScoringMethod.ALL_OR_NOTHING, List.of(), null, null, null);

        assertThatThrownBy(() -> service.addQuestion("instructor-1", "course-1", "quiz-1", noOptions))
                .isInstanceOf(BadRequestException.class);

        verify(quizQuestionRepository, never()).save(any());
    }

    @Test
    void anEssayStoresNoOptionsAndKeepsItsReferenceAnswer() {
        when(quizLessonRepository.findByLessonId("quiz-1")).thenReturn(Optional.of(QuizLesson.builder().lessonId("quiz-1").build()));
        ArgumentCaptor<QuizQuestion> saved = ArgumentCaptor.forClass(QuizQuestion.class);
        when(quizQuestionRepository.save(saved.capture())).thenAnswer(invocation -> {
            QuizQuestion question = invocation.getArgument(0);
            question.setId("question-3");
            return question;
        });
        when(quizOptionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.addQuestion("instructor-1", "course-1", "quiz-1", essay());

        assertThat(saved.getValue().getReferenceAnswer()).isEqualTo("Mention the cache.");
        assertThat(saved.getValue().getOptions()).isEmpty();
        assertThat(saved.getValue().getAiAssisted()).isFalse();
    }

    @Test
    void aQuestionDeclaredAiAssistedKeepsTheLabel() {
        when(quizLessonRepository.findByLessonId("quiz-1")).thenReturn(Optional.of(QuizLesson.builder().lessonId("quiz-1").build()));
        ArgumentCaptor<QuizQuestion> saved = ArgumentCaptor.forClass(QuizQuestion.class);
        when(quizQuestionRepository.save(saved.capture())).thenAnswer(invocation -> {
            QuizQuestion question = invocation.getArgument(0);
            question.setId("question-1");
            return question;
        });
        when(quizOptionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.addQuestion("instructor-1", "course-1", "quiz-1", single());

        assertThat(saved.getValue().getAiAssisted()).isTrue();
    }

    private static CreateQuizQuestionCommand single() {
        return new CreateQuizQuestionCommand("2+2?", QuizQuestionType.SINGLE_CHOICE, ScoringMethod.ALL_OR_NOTHING, List.of(
                new CreateQuizOptionCommand("3", false, 1, null),
                new CreateQuizOptionCommand("4", true, 2, null)), null, "2 + 2 is 4.", true);
    }

    private static CreateQuizQuestionCommand essay() {
        return new CreateQuizQuestionCommand("Explain.", QuizQuestionType.ESSAY, ScoringMethod.ALL_OR_NOTHING, null, "Mention the cache.", null,
                null);
    }
}
