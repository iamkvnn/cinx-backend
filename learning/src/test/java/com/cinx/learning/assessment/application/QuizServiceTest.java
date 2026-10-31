package com.cinx.learning.assessment.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.assessment.application.command.ChooseQuizAnswerCommand;
import com.cinx.learning.assessment.application.command.GradeEssayCommand;
import com.cinx.learning.assessment.application.command.SubmitQuizSessionCommand;
import com.cinx.learning.assessment.application.evaluator.QuestionEvaluatorFactory;
import com.cinx.learning.assessment.application.mapper.QuizSessionMapper;
import com.cinx.learning.assessment.application.query.QuizSessionQuestionView;
import com.cinx.learning.assessment.application.query.QuizSessionSubmissionView;
import com.cinx.learning.assessment.application.query.QuizSessionView;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import com.cinx.learning.assessment.domain.QuizSessionQuestionRepository;
import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import com.cinx.learning.assessment.domain.QuizSessionSubmissionRepository;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.courseview.application.query.QuizLessonView;
import com.cinx.learning.courseview.application.query.QuizQuestionView;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.progress.application.ILearningProgressService;
import com.cinx.learning.progress.application.LearningItemProgressUpdateResult;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import com.cinx.learning.shared.domain.ScoringMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.cinx.learning.review.application.IReviewService;
import com.cinx.learning.review.application.command.QuizQuestionResult;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock
    private QuizScoreAggregator quizScoreAggregator;
    @Mock
    private QuestionEvaluatorFactory questionEvaluatorFactory;
    @Mock
    private QuizSessionRepository quizSessionRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private QuizSessionMapper quizSessionMapper;
    @Mock
    private QuizSessionSubmissionRepository quizSessionSubmissionRepository;
    @Mock
    private QuizSessionQuestionRepository quizSessionQuestionRepository;
    @Mock
    private ILearningProgressService learningProgressService;
    @Mock
    private IDailyGoalService dailyGoalService;
    @Mock
    private QuizSnapshotBuilder snapshotBuilder;
    @Mock
    private IReviewService reviews;

    @InjectMocks
    private QuizService quizService;

    @Test
    void submitSendsTheGradedNonEssayQuestionsToSpacedReview() {
        QuizSession session = session(QuizSessionStatus.IN_PROGRESS);
        QuizSessionQuestion wrong = QuizSessionQuestion.builder().quizSessionId("session-1").questionId("q-1")
                .questionType(QuizQuestionType.SINGLE_CHOICE).correctAnswer("a").userAnswer("b").build();
        QuizSessionQuestion partial = QuizSessionQuestion.builder().quizSessionId("session-1").questionId("q-2")
                .questionType(QuizQuestionType.MULTI_CHOICE).correctAnswer("a,b").userAnswer("a").build();
        QuizSessionQuestion essay = QuizSessionQuestion.builder().quizSessionId("session-1").questionId("q-3")
                .questionType(QuizQuestionType.ESSAY).userAnswer("text").build();
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(quizSessionQuestionRepository.findAllByQuizSessionId(eq("session-1"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(wrong, partial, essay), 1, 3, 3));
        when(questionEvaluatorFactory.resolve(wrong)).thenReturn(q -> 0.0);
        when(questionEvaluatorFactory.resolve(partial)).thenReturn(q -> 0.5);
        when(questionEvaluatorFactory.resolve(essay)).thenReturn(q -> 0.0);
        when(quizSessionSubmissionRepository.save(any(QuizSessionSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quizSessionMapper.toDto(eq(session), any())).thenReturn(view("session-1", QuizSessionStatus.PENDING_GRADE, null));

        quizService.submitQuizSession("session-1", new SubmitQuizSessionCommand(List.of()));

        verify(reviews).recordQuizResults("user-1", "course-1", "quiz-1",
                List.of(new QuizQuestionResult("q-1", 0.0), new QuizQuestionResult("q-2", 0.5)));
    }

    @Test
    void aQuizThatHidesItsAnswersSendsNothingToReview() {
        QuizSession session = session(QuizSessionStatus.IN_PROGRESS);
        session.setIsShowAnswersOnReview(false);
        QuizSessionQuestion wrong = QuizSessionQuestion.builder().quizSessionId("session-1").questionId("q-1")
                .questionType(QuizQuestionType.SINGLE_CHOICE).correctAnswer("a").userAnswer("b").build();
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(quizSessionQuestionRepository.findAllByQuizSessionId(eq("session-1"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(wrong), 1, 1, 1));
        when(questionEvaluatorFactory.resolve(wrong)).thenReturn(q -> 0.0);
        when(quizSessionSubmissionRepository.save(any(QuizSessionSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quizScoreAggregator.aggregateScore("course-1", "user-1", "quiz-1")).thenReturn(0.0);
        when(learningProgressService.updateLearningItemProgress("user-1", "quiz-1", new UpdateLearningItemCommand(true, false, 0.0)))
                .thenReturn(new LearningItemProgressUpdateResult(false, false, false, false));
        when(quizSessionMapper.toDto(eq(session), any())).thenReturn(view("session-1", QuizSessionStatus.SUBMITTED, null));

        quizService.submitQuizSession("session-1", new SubmitQuizSessionCommand(List.of()));

        org.mockito.Mockito.verifyNoInteractions(reviews);
    }

    @Test
    void getQuizSessionsHandsTheSortParameterToTheStore() {
        ArgumentCaptor<PageQuery> query = ArgumentCaptor.forClass(PageQuery.class);
        when(quizSessionRepository.findAllByQuizLessonId(eq("quiz-1"), eq("user-1"), query.capture()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));

        quizService.getQuizSessions("user-1", "quiz-1", 1, 10, "{\"startTime\":\"DESC\"}");

        assertThat(query.getValue().sort()).isEqualTo("{\"startTime\":\"DESC\"}");
        assertThat(query.getValue().page()).isEqualTo(1);
        assertThat(query.getValue().size()).isEqualTo(10);
    }

    @Test
    void getQuizSessionsAttachesTheSubmissionOfEachSession() {
        QuizSession first = session(QuizSessionStatus.GRADED);
        first.setId("session-1");
        QuizSession second = session(QuizSessionStatus.IN_PROGRESS);
        second.setId("session-2");
        QuizSessionSubmission submission = QuizSessionSubmission.builder().quizSessionId("session-1").score(7.5).build();
        QuizSessionView firstView = view("session-1", QuizSessionStatus.GRADED, null);
        QuizSessionView secondView = view("session-2", QuizSessionStatus.IN_PROGRESS, null);
        when(quizSessionRepository.findAllByQuizLessonId(eq("quiz-1"), eq("user-1"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(first, second), 1, 10, 2));
        when(quizSessionSubmissionRepository.findAllByQuizSessionIdIn(List.of("session-1", "session-2")))
                .thenReturn(List.of(submission));
        when(quizSessionMapper.toDto(first, submission)).thenReturn(firstView);
        when(quizSessionMapper.toDto(second, null)).thenReturn(secondView);

        PageResult<QuizSessionView> result = quizService.getQuizSessions("user-1", "quiz-1", 1, 10, null);

        assertThat(result.content()).containsExactly(firstView, secondView);
    }

    @Test
    void createQuizSessionRejectsExistingInProgressSession() {
        when(courseService.getQuizLessonById("course-1", "quiz-1"))
                .thenReturn(new ApiResponse<>(true, "ok", quizLesson()));
        when(quizSessionRepository.existsByQuizLessonIdAndUserIdAndStatus(
                "quiz-1",
                "user-1",
                QuizSessionStatus.IN_PROGRESS))
                .thenReturn(true);

        assertThatThrownBy(() -> quizService.createQuizSession("course-1", "user-1", "quiz-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("in-progress");
    }

    @Test
    void createQuizSessionForAQuizWithoutATimeLimitHasNoEndTime() {
        // the editor offers "Không giới hạn" as a duration; the session must start, with no deadline
        when(courseService.getQuizLessonById("course-1", "quiz-1"))
                .thenReturn(new ApiResponse<>(true, "ok", new QuizLessonView(1, 3, null, true, true, false, false, null, List.of())));
        when(quizSessionRepository.save(any(QuizSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quizService.createQuizSession("course-1", "user-1", "quiz-1");

        ArgumentCaptor<QuizSession> saved = ArgumentCaptor.forClass(QuizSession.class);
        verify(quizSessionRepository).save(saved.capture());
        assertThat(saved.getValue().getEndTime()).isNull();
        assertThat(saved.getValue().getStatus()).isEqualTo(QuizSessionStatus.IN_PROGRESS);
    }

    @Test
    void chooseAnswerInASessionWithoutADeadlineIsAccepted() {
        QuizSession session = session(QuizSessionStatus.IN_PROGRESS);
        session.setEndTime(null);
        QuizSessionQuestion question = QuizSessionQuestion.builder().quizSessionId("session-1").questionId("q-1").build();
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(quizSessionQuestionRepository.findByQuizSessionIdAndQuestionId("session-1", "q-1")).thenReturn(Optional.of(question));

        quizService.chooseQuizSessionQuestion("session-1", new ChooseQuizAnswerCommand("q-1", "a"));

        assertThat(question.getUserAnswer()).isEqualTo("a");
    }

    @Test
    void submitQuizSessionRejectsDuplicateAnswers() {
        QuizSession session = session(QuizSessionStatus.IN_PROGRESS);
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        SubmitQuizSessionCommand request = new SubmitQuizSessionCommand(List.of(
                new ChooseQuizAnswerCommand("q-1", "a"),
                new ChooseQuizAnswerCommand("q-1", "b")));

        assertThatThrownBy(() -> quizService.submitQuizSession("session-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate answer");
    }

    @Test
    void gradeEssayMovesSessionToGradedAndRecordsQuizPassedTransition() {
        QuizSession session = session(QuizSessionStatus.PENDING_GRADE);
        QuizSessionQuestion essay = QuizSessionQuestion.builder()
                .quizSessionId("session-1")
                .questionId("essay-1")
                .questionType(QuizQuestionType.ESSAY)
                .score(0.0)
                .build();
        QuizSessionSubmission submission = QuizSessionSubmission.builder()
                .quizSessionId("session-1")
                .score(0.0)
                .build();
        QuizSessionView response = view("session-1", QuizSessionStatus.GRADED, null);

        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(quizSessionQuestionRepository.findAllEssayByQuizSessionId("session-1")).thenReturn(List.of(essay));
        when(quizSessionQuestionRepository.findAllByQuizSessionId(any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(essay), 1, 1, 1));
        when(quizSessionSubmissionRepository.findByQuizSessionId("session-1")).thenReturn(Optional.of(submission));
        when(quizSessionSubmissionRepository.save(submission)).thenReturn(submission);
        when(quizScoreAggregator.aggregateScore("course-1", "user-1", "quiz-1")).thenReturn(8.0);
        when(learningProgressService.updateLearningItemProgress(
                "user-1",
                "quiz-1",
                new UpdateLearningItemCommand(true, true, 8.0)))
                .thenReturn(new LearningItemProgressUpdateResult(false, true, false, false));
        when(quizSessionMapper.toDto(session, submission)).thenReturn(response);

        QuizSessionView result = quizService.gradeEssay(
                "session-1",
                new GradeEssayCommand(List.of(new GradeEssayCommand.EssayQuestionScore("essay-1", 8.0))));

        assertThat(session.getStatus()).isEqualTo(QuizSessionStatus.GRADED);
        assertThat(result.status()).isEqualTo(QuizSessionStatus.GRADED);
        // 8.0 on the essay's 0-10 scale is stored as 0.8 of the question, and the submission carries the raw 8.0
        assertThat(essay.getScore()).isEqualTo(0.8);
        assertThat(submission.getScore()).isEqualTo(8.0);
        verify(dailyGoalService).recordProgress("user-1", DailyGoalType.QUIZZES_PASSED, 1);
    }

    @Test
    void submitQuizSessionStoresUserIdOnSubmission() {
        QuizSession session = session(QuizSessionStatus.IN_PROGRESS);
        QuizSessionQuestion question = QuizSessionQuestion.builder()
                .quizSessionId("session-1")
                .questionId("q-1")
                .questionType(QuizQuestionType.SINGLE_CHOICE)
                .correctAnswer("a")
                .userAnswer("a")
                .build();
        QuizSessionView response = view("session-1", QuizSessionStatus.SUBMITTED, null);

        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(quizSessionQuestionRepository.findAllByQuizSessionId(eq("session-1"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(question), 1, 1, 1));
        when(questionEvaluatorFactory.resolve(question)).thenReturn(q -> 1.0);
        when(quizSessionSubmissionRepository.save(any(QuizSessionSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(quizScoreAggregator.aggregateScore("course-1", "user-1", "quiz-1")).thenReturn(10.0);
        when(learningProgressService.updateLearningItemProgress(
                "user-1",
                "quiz-1",
                new UpdateLearningItemCommand(true, true, 10.0)))
                .thenReturn(new LearningItemProgressUpdateResult(false, true, false, false));
        when(quizSessionMapper.toDto(eq(session), any())).thenReturn(response);

        QuizSessionView result = quizService.submitQuizSession("session-1", new SubmitQuizSessionCommand(List.of()));

        assertThat(result.userId()).isEqualTo("user-1");
        verify(quizSessionSubmissionRepository).save(argThat(submission ->
                "user-1".equals(submission.getUserId())
                        && "session-1".equals(submission.getQuizSessionId())
                        && submission.getSubmissionTime() != null));
    }

    @Test
    void gradedSessionReviewHonorsReviewAllowedAndShowsAnswersWhenAllowed() {
        QuizSession blocked = session(QuizSessionStatus.GRADED);
        blocked.setIsReviewAllowed(false);
        when(quizSessionRepository.findById("blocked")).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> quizService.getQuizSessionQuestions("blocked", 1, 10, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Review is not allowed");

        QuizSession allowed = session(QuizSessionStatus.GRADED);
        QuizSessionQuestion question = QuizSessionQuestion.builder()
                .id("session-question-1")
                .quizSessionId("allowed")
                .questionId("q-1")
                .questionType(QuizQuestionType.SINGLE_CHOICE)
                .questionOrder(1)
                .questionText("Question?")
                .userAnswer("a")
                .correctAnswer("b")
                .score(0.0)
                .build();
        when(quizSessionRepository.findById("allowed")).thenReturn(Optional.of(allowed));
        when(quizSessionQuestionRepository.findAllByQuizSessionId(eq("allowed"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(question), 1, 10, 1));
        when(snapshotBuilder.parseOptionsSnapshot(null)).thenReturn(List.of());

        PageResult<QuizSessionQuestionView> response = quizService.getQuizSessionQuestions("allowed", 1, 10, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().correctAnswer()).isEqualTo("b");
        assertThat(response.content().getFirst().score()).isEqualTo(0.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theAttemptCopiesTheAiLabelOfEachQuestion() {
        QuizQuestionView aiWritten = new QuizQuestionView("q-1", "2+2?", QuizQuestionType.SINGLE_CHOICE,
                ScoringMethod.ALL_OR_NOTHING, List.of(), null, true);
        QuizQuestionView handWritten = new QuizQuestionView("q-2", "3+3?", QuizQuestionType.SINGLE_CHOICE,
                ScoringMethod.ALL_OR_NOTHING, List.of(), null, null);
        when(courseService.getQuizLessonById("course-1", "quiz-1")).thenReturn(new ApiResponse<>(true, "ok",
                new QuizLessonView(null, 3, 30, true, true, false, false, null, List.of(aiWritten, handWritten))));
        when(quizSessionRepository.save(any(QuizSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quizService.createQuizSession("course-1", "user-1", "quiz-1");

        ArgumentCaptor<List<QuizSessionQuestion>> saved = ArgumentCaptor.forClass(List.class);
        verify(quizSessionQuestionRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(QuizSessionQuestion::getAiAssisted).containsExactly(true, false);
    }

    @Test
    void theAiLabelShowsWhileTheAnswersAreStillHidden() {
        QuizSession inProgress = session(QuizSessionStatus.IN_PROGRESS);
        QuizSessionQuestion question = QuizSessionQuestion.builder()
                .id("session-question-1").quizSessionId("session-1").questionId("q-1").correctAnswer("b")
                .aiAssisted(true).build();
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(inProgress));
        when(quizSessionQuestionRepository.findAllByQuizSessionId(eq("session-1"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(question), 1, 10, 1));

        QuizSessionQuestionView view = quizService.getQuizSessionQuestions("session-1", 1, 10, null).content().getFirst();

        assertThat(view.correctAnswer()).isNull();
        assertThat(view.aiAssisted()).isTrue();
    }

    private QuizSession session(QuizSessionStatus status) {
        return QuizSession.builder()
                .id("session-1")
                .courseId("course-1")
                .userId("user-1")
                .quizLessonId("quiz-1")
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(LocalDateTime.now().plusMinutes(10))
                .status(status)
                .isReviewAllowed(true)
                .isShowAnswersOnReview(true)
                .build();
    }

    private QuizSessionView view(String id, QuizSessionStatus status, QuizSessionSubmissionView submission) {
        return new QuizSessionView(id, "user-1", "quiz-1", LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10),
                status, true, true, submission);
    }

    private QuizLessonView quizLesson() {
        return new QuizLessonView(1, 3, 30, true, true, false, false, null, List.of());
    }
}
