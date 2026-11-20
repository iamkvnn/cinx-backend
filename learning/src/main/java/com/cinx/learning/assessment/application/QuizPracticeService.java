package com.cinx.learning.assessment.application;

import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.assessment.application.evaluator.QuestionEvaluatorFactory;
import com.cinx.learning.assessment.application.query.PracticeCheckView;
import com.cinx.learning.assessment.application.query.PracticeQuestionView;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.courseview.application.query.QuizLessonView;
import com.cinx.learning.courseview.application.query.QuizQuestionView;
import com.cinx.learning.shared.domain.QuizQuestionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizPracticeService implements QuizPractice {
    private final CourseService courseService;
    private final QuizSnapshotBuilder snapshotBuilder;
    private final QuestionEvaluatorFactory evaluators;

    @Override
    public Optional<Map<String, PracticeQuestionView>> questions(String courseId, String lessonId) {
        return readableQuiz(courseId, lessonId).map(quiz -> {
            Map<String, PracticeQuestionView> questions = new LinkedHashMap<>();
            if (QuizPractice.reviewable(quiz.isReviewAllowed(), quiz.isShowAnswersOnReview()) && quiz.questions() != null) {
                quiz.questions().stream()
                        .filter(QuizPracticeService::practisable)
                        .forEach(q -> questions.put(q.id(), new PracticeQuestionView(q.id(), q.questionText(), q.questionType(),
                                q.scoringMethod(), snapshotBuilder.parseOptionsSnapshot(snapshotBuilder.buildOptionsSnapshot(q, true)))));
            }
            return questions;
        });
    }

    @Override
    public Optional<PracticeCheckView> check(String courseId, String lessonId, String questionId, String userAnswer) {
        return readableQuiz(courseId, lessonId)
                .filter(quiz -> QuizPractice.reviewable(quiz.isReviewAllowed(), quiz.isShowAnswersOnReview()) && quiz.questions() != null)
                .flatMap(quiz -> quiz.questions().stream().filter(q -> q.id().equals(questionId)).findFirst())
                .filter(QuizPracticeService::practisable)
                .map(q -> {
                    // the same snapshot a quiz session takes, so review and quiz agree on what is right
                    QuizSessionQuestion graded = QuizSessionQuestion.builder()
                            .questionId(q.id())
                            .questionType(q.questionType())
                            .questionText(q.questionText())
                            .scoringMethod(q.scoringMethod())
                            .correctAnswer(snapshotBuilder.buildCorrectAnswer(q))
                            .optionsSnapshot(snapshotBuilder.buildOptionsSnapshot(q, false))
                            .userAnswer(userAnswer)
                            .build();
                    double score = evaluators.resolve(graded).evaluate(graded);
                    return new PracticeCheckView(score, graded.getCorrectAnswer(), q.explanation());
                });
    }

    /** The quiz as the signed-in learner reads it; empty when they cannot read the lesson now. */
    private Optional<QuizLessonView> readableQuiz(String courseId, String lessonId) {
        try {
            return Optional.ofNullable(courseService.getQuizLessonById(courseId, lessonId).data());
        } catch (NotFoundException | ForbiddenException noAccess) {
            return Optional.empty();
        }
    }

    private static boolean practisable(QuizQuestionView question) {
        return question.questionType() != QuizQuestionType.ESSAY;
    }
}
