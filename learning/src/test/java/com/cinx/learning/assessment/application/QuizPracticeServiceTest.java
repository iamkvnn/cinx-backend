package com.cinx.learning.assessment.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.assessment.application.evaluator.QuestionEvaluatorFactory;
import com.cinx.learning.assessment.application.query.PracticeQuestionView;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.courseview.application.query.QuizLessonView;
import com.cinx.learning.courseview.application.query.QuizOptionView;
import com.cinx.learning.courseview.application.query.QuizQuestionView;
import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuizPracticeServiceTest {
    @Mock private CourseService courseService;
    @Mock private QuestionEvaluatorFactory evaluators;

    private QuizPracticeService practice;

    @BeforeEach
    void setUp() {
        practice = new QuizPracticeService(courseService, new QuizSnapshotBuilder(new ObjectMapper()), evaluators);
    }

    @Test
    void reviewableQuestionsCarryNoKeyAndNoEssay() {
        doReturn(new ApiResponse<>(true, "ok", quiz(true, true))).when(courseService).getQuizLessonById("c1", "l1");

        Map<String, PracticeQuestionView> questions = practice.questions("c1", "l1").orElseThrow();

        assertThat(questions).containsOnlyKeys("q1");
        assertThat(questions.get("q1").options()).extracting("optionText").containsExactlyInAnyOrder("Đúng", "Sai");
    }

    @Test
    void aQuizThatHidesAnswersHasNothingToReview() {
        doReturn(new ApiResponse<>(true, "ok", quiz(true, false))).when(courseService).getQuizLessonById("c1", "l1");

        assertThat(practice.questions("c1", "l1")).contains(Map.of());
        assertThat(practice.check("c1", "l1", "q1", "[\"o1\"]")).isEmpty();
    }

    @Test
    void aLessonTheLearnerCannotReadIsEmptyRatherThanAnError() {
        doThrow(new NotFoundException("Lesson not found")).when(courseService).getQuizLessonById("c1", "l1");

        assertThat(practice.questions("c1", "l1")).isEmpty();
    }

    @Test
    void anAnswerIsGradedByTheQuizsOwnEvaluatorOnTheSessionSnapshot() {
        doReturn(new ApiResponse<>(true, "ok", quiz(true, true))).when(courseService).getQuizLessonById("c1", "l1");
        ArgumentCaptor<QuizSessionQuestion> graded = ArgumentCaptor.forClass(QuizSessionQuestion.class);
        doReturn((com.cinx.learning.assessment.application.evaluator.IQuestionEvaluator) q -> 1.0).when(evaluators).resolve(any());

        var checked = practice.check("c1", "l1", "q1", "[\"o1\"]").orElseThrow();

        verify(evaluators).resolve(graded.capture());
        assertThat(graded.getValue().getUserAnswer()).isEqualTo("[\"o1\"]");
        assertThat(graded.getValue().getCorrectAnswer()).isNotBlank();
        assertThat(checked.correct()).isTrue();
        assertThat(checked.explanation()).isEqualTo("Vì vậy");
    }

    private static QuizLessonView quiz(boolean reviewAllowed, boolean showAnswers) {
        QuizQuestionView choice = new QuizQuestionView("q1", "1 + 1 = 2?", QuizQuestionType.SINGLE_CHOICE, ScoringMethod.ALL_OR_NOTHING,
                List.of(new QuizOptionView("o1", "Đúng", true, 1, null, null), new QuizOptionView("o2", "Sai", false, 2, null, null)),
                "Vì vậy", false);
        QuizQuestionView essay = new QuizQuestionView("q2", "Viết một đoạn", QuizQuestionType.ESSAY, ScoringMethod.ALL_OR_NOTHING,
                List.of(), null, false);
        return new QuizLessonView(2, null, null, reviewAllowed, showAnswers, false, false, null, List.of(choice, essay));
    }
}
