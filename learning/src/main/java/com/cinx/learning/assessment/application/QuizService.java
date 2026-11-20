package com.cinx.learning.assessment.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.learning.assessment.application.query.QuizQuestionAnalyticsView;
import com.cinx.learning.assessment.application.query.QuizSessionOptionView;
import com.cinx.learning.assessment.application.query.QuizSessionQuestionView;
import com.cinx.learning.assessment.application.query.QuizSessionView;
import com.cinx.learning.courseview.application.query.QuizLessonView;
import com.cinx.learning.courseview.application.query.QuizQuestionView;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import com.cinx.learning.assessment.application.command.ChooseQuizAnswerCommand;
import com.cinx.learning.assessment.application.command.GradeEssayCommand;
import com.cinx.learning.assessment.application.command.SubmitQuizSessionCommand;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.assessment.application.mapper.QuizSessionMapper;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import com.cinx.learning.assessment.domain.QuizSessionQuestionRepository;
import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.assessment.domain.QuizSessionSubmissionRepository;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.progress.application.ILearningProgressService;
import com.cinx.learning.progress.application.LearningItemProgressUpdateResult;
import com.cinx.learning.assessment.application.evaluator.IQuestionEvaluator;
import com.cinx.learning.assessment.application.evaluator.QuestionEvaluatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cinx.learning.review.application.IReviewService;
import com.cinx.learning.review.application.command.QuizQuestionResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService implements IQuizService {
    private static final double PASSING_SCORE = 5.0;

    private final QuizScoreAggregator quizScoreAggregator;
    private final QuestionEvaluatorFactory questionEvaluatorFactory;
    private final QuizSessionRepository quizSessionRepository;
    private final CourseService courseService;
    private final QuizSessionMapper quizSessionMapper;
    private final QuizSessionSubmissionRepository quizSessionSubmissionRepository;
    private final QuizSessionQuestionRepository quizSessionQuestionRepository;
    private final ILearningProgressService learningProgressService;
    private final IDailyGoalService dailyGoalService;
    private final QuizSnapshotBuilder snapshotBuilder;
    private final IReviewService reviews;

    @Override
    public PageResult<QuizSessionView> getQuizSessions(String userId, String lessonId, int page, int size, String sort) {
        validatePageRequest(page, size);
        PageResult<QuizSession> sessions = quizSessionRepository.findAllByQuizLessonId(lessonId, userId, new PageQuery(page, size, sort));
        Map<String, QuizSessionSubmission> submissions = quizSessionSubmissionRepository
                .findAllByQuizSessionIdIn(sessions.content().stream().map(QuizSession::getId).toList()).stream()
                .collect(Collectors.toMap(QuizSessionSubmission::getQuizSessionId, Function.identity(), (a, b) -> a));
        return sessions.map(session -> quizSessionMapper.toDto(session, submissions.get(session.getId())));
    }

    private QuizSessionView toView(QuizSession session) {
        return quizSessionMapper.toDto(session, quizSessionSubmissionRepository.findByQuizSessionId(session.getId()).orElse(null));
    }

    @Override
    public QuizSessionView getQuizSession(String id) {
        return quizSessionRepository.findById(id)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));
    }

    @Override
    public PageResult<QuizSessionQuestionView> getQuizSessionQuestions(String quizSessionId, int page, int size, String sort) {
        validatePageRequest(page, size);
        QuizSession quizSession = quizSessionRepository.findById(quizSessionId)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));

        if (isReviewStatus(quizSession.getStatus())) {
            if (Boolean.FALSE.equals(quizSession.getIsReviewAllowed())) {
                throw new BadRequestException(ErrorCode.QUIZ_REVIEW_NOT_ALLOWED, "Review is not allowed for this quiz session");
            }
        }

        boolean inProgress = quizSession.getStatus() == QuizSessionStatus.IN_PROGRESS;
        boolean hideAnswers = Boolean.FALSE.equals(quizSession.getIsShowAnswersOnReview());

        return quizSessionQuestionRepository
                .findAllByQuizSessionId(quizSessionId, new PageQuery(page, size, sort))
                .map(q -> buildResponse(q, inProgress || hideAnswers));
    }

    private QuizSessionQuestionView buildResponse(QuizSessionQuestion q, boolean hideAnswers) {
        List<QuizSessionOptionView> options = snapshotBuilder.parseOptionsSnapshot(q.getOptionsSnapshot());
        return new QuizSessionQuestionView(
                q.getId(),
                q.getQuizSessionId(),
                q.getQuestionId(),
                q.getQuestionType(),
                q.getScoringMethod(),
                q.getQuestionOrder(),
                q.getQuestionText(),
                q.getUserAnswer(),
                hideAnswers ? null : q.getCorrectAnswer(),
                hideAnswers ? null : q.getScore(),
                options,
                hideAnswers ? null : q.getExplanation(),
                Boolean.TRUE.equals(q.getAiAssisted()));
    }

    @Transactional
    @Override
    public QuizSessionView createQuizSession(String courseId, String userId, String lessonId) {
        QuizLessonView quizLessonResponse = courseService.getQuizLessonById(courseId, lessonId).data();

        if (quizSessionRepository.existsByQuizLessonIdAndUserIdAndStatus(lessonId, userId, QuizSessionStatus.IN_PROGRESS)) {
            throw new BadRequestException(ErrorCode.QUIZ_SESSION_ALREADY_IN_PROGRESS, "You already have an in-progress quiz session for this lesson");
        }

        Integer maxAttempt = quizLessonResponse.maxAttempt();
        if (maxAttempt != null && maxAttempt <= quizSessionRepository.countByQuizLessonIdAndUserId(lessonId, userId)) {
            throw new BadRequestException(ErrorCode.QUIZ_ATTEMPT_LIMIT_REACHED, "You have reached the maximum number of attempts for this quiz lesson");
        }

        QuizSession quizSession = quizSessionRepository.save(
                QuizSession.builder()
                        .courseId(courseId)
                        .userId(userId)
                        .startTime(LocalDateTime.now())
                        // a quiz without a time limit has no end time (the editor allows "Không giới hạn")
                        .endTime(quizLessonResponse.duration() == null ? null : LocalDateTime.now().plusMinutes(quizLessonResponse.duration()))
                        .quizLessonId(lessonId)
                        .status(QuizSessionStatus.IN_PROGRESS)
                        .isReviewAllowed(quizLessonResponse.isReviewAllowed())
                        .isShowAnswersOnReview(quizLessonResponse.isShowAnswersOnReview())
                        .build());

        createQuizSessionQuestions(
                quizSession,
                quizLessonResponse.numberOfQuestionPerQuizSession(),
                quizLessonResponse.questions(),
                Boolean.TRUE.equals(quizLessonResponse.shuffleQuestions()),
                Boolean.TRUE.equals(quizLessonResponse.shuffleOptions()));

        return toView(quizSession);
    }

    private void createQuizSessionQuestions(
            QuizSession quizSession,
            Integer numberOfQuestionPerQuizSession,
            List<QuizQuestionView> questions,
            boolean shuffleQuestions,
            boolean shuffleOptions) {
        List<QuizQuestionView> pool = new ArrayList<>(questions);
        if (shuffleQuestions)
            Collections.shuffle(pool);
        int requestedQuestionCount = numberOfQuestionPerQuizSession != null
                ? numberOfQuestionPerQuizSession
                : pool.size();
        List<QuizQuestionView> selected = pool.subList(0, Math.min(requestedQuestionCount, pool.size()));

        List<QuizSessionQuestion> sessionQuestions = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            QuizQuestionView q = selected.get(i);
            sessionQuestions.add(QuizSessionQuestion.builder()
                    .quizSessionId(quizSession.getId())
                    .questionOrder(i + 1)
                    .questionType(q.questionType())
                    .questionId(q.id())
                    .questionText(q.questionText())
                    .scoringMethod(q.scoringMethod())
                    .correctAnswer(snapshotBuilder.buildCorrectAnswer(q))
                    .optionsSnapshot(snapshotBuilder.buildOptionsSnapshot(q, shuffleOptions))
                    .explanation(q.explanation())
                    .aiAssisted(Boolean.TRUE.equals(q.aiAssisted()))
                    .build());
        }

        quizSessionQuestionRepository.saveAll(sessionQuestions);
    }

    @Override
    @Transactional
    public void chooseQuizSessionQuestion(String quizSessionId, ChooseQuizAnswerCommand request) {
        QuizSession quizSession = quizSessionRepository.findById(quizSessionId)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));
        if (quizSession.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            throw new BadRequestException(ErrorCode.QUIZ_SESSION_NOT_IN_PROGRESS, "Quiz session is not in progress");
        }
        if (quizSession.getEndTime() != null && quizSession.getEndTime().isBefore(LocalDateTime.now())) {
            submitQuizSession(quizSessionId, new SubmitQuizSessionCommand(Collections.emptyList()));
            throw new BadRequestException(ErrorCode.QUIZ_SESSION_EXPIRED, "Quiz session has expired and was automatically submitted");
        }

        quizSessionQuestionRepository.findByQuizSessionIdAndQuestionId(quizSessionId, request.questionId())
                .ifPresentOrElse(
                        q -> {
                            q.setUserAnswer(request.userAnswer());
                            quizSessionQuestionRepository.save(q);
                        },
                        () -> {
                            throw new NotFoundException("Quiz session question not found");
                        });
    }

    @Transactional
    @Override
    public QuizSessionView submitQuizSession(String quizSessionId, SubmitQuizSessionCommand request) {
        QuizSession quizSession = quizSessionRepository.findById(quizSessionId)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));

        if (quizSession.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            throw new BadRequestException(ErrorCode.QUIZ_SESSION_NOT_IN_PROGRESS, "Quiz session is not in progress");
        }
        
        boolean isExpired = quizSession.getEndTime() != null && quizSession.getEndTime().isBefore(LocalDateTime.now());
        
        Map<String, ChooseQuizAnswerCommand> answerMap = !isExpired
                ? buildAnswerMap(request)
                : Collections.emptyMap();

        List<QuizSessionQuestion> questions = quizSessionQuestionRepository.findAllByQuizSessionId(quizSessionId, PageQuery.unpaged()).content();
        questions.forEach(q -> {
            ChooseQuizAnswerCommand lastAnswer = answerMap.get(q.getQuestionId());
            if (lastAnswer != null)
                q.setUserAnswer(lastAnswer.userAnswer());
        });

        boolean hasEssay = false;
        double totalScore = 0.0;
        int correctCount = 0;

        for (QuizSessionQuestion q : questions) {
            if (q.getQuestionType() == QuizQuestionType.ESSAY) {
                hasEssay = true;
            }
            IQuestionEvaluator evaluator = questionEvaluatorFactory.resolve(q);
            double fraction = evaluator.evaluate(q);
            q.setScore(fraction);
            totalScore += fraction;
            if (fraction >= 1.0) {
                correctCount++;
            }
            log.debug("Graded question {} | type={} | method={} | score={}",
                    q.getQuestionId(), q.getQuestionType(), q.getScoringMethod(), fraction);
        }

        quizSessionQuestionRepository.saveAll(questions);

        // missed questions come back for spaced review, only where the quiz lets the learner see the answers
        if (QuizPractice.reviewable(quizSession.getIsReviewAllowed(), quizSession.getIsShowAnswersOnReview())) {
            reviews.recordQuizResults(quizSession.getUserId(), quizSession.getCourseId(), quizSession.getQuizLessonId(),
                    questions.stream()
                            .filter(q -> q.getQuestionType() != QuizQuestionType.ESSAY && q.getScore() != null)
                            .map(q -> new QuizQuestionResult(q.getQuestionId(), q.getScore()))
                            .toList());
        }

        quizSession.setStatus(hasEssay ? QuizSessionStatus.PENDING_GRADE : QuizSessionStatus.SUBMITTED);
        quizSessionRepository.save(quizSession);

        double rawScore = questions.isEmpty() ? 0.0 : totalScore / questions.size() * 10.0;

        quizSessionSubmissionRepository.save(
                QuizSessionSubmission.builder()
                        .userId(quizSession.getUserId())
                        .quizSessionId(quizSession.getId())
                        .score(rawScore)
                        .submissionTime(LocalDateTime.now())
                        .totalCorrectAnswers(correctCount)
                        .build());

        if (!hasEssay) {
            double effectiveScore = quizScoreAggregator.aggregateScore(
                    quizSession.getCourseId(),
                    quizSession.getUserId(),
                    quizSession.getQuizLessonId());

            log.info("Quiz session {} graded. rawScore={} effectiveScore={}", quizSessionId, rawScore, effectiveScore);

            boolean isPassed = effectiveScore >= PASSING_SCORE;

            LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                    quizSession.getUserId(),
                    quizSession.getQuizLessonId(),
                    new UpdateLearningItemCommand(true, isPassed, effectiveScore));

            if (result.passedTransition()) {
                dailyGoalService.recordProgress(quizSession.getUserId(), DailyGoalType.QUIZZES_PASSED, 1);
            }
        } else {
            log.info("Quiz session {} pending essay grading. rawScore={}", quizSessionId, rawScore);
        }

        return toView(quizSession);
    }

    @Override
    @Transactional
    public QuizSessionView gradeEssay(String sessionId, GradeEssayCommand request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));

        if (session.getStatus() != QuizSessionStatus.PENDING_GRADE) {
            throw new BadRequestException(ErrorCode.QUIZ_SESSION_NOT_PENDING_GRADING, "Quiz session is not pending essay grading");
        }

        Map<String, Double> scoreMap = buildEssayScoreMap(request);

        List<QuizSessionQuestion> questions = quizSessionQuestionRepository.findAllEssayByQuizSessionId(sessionId);
        questions.stream()
                .filter(q -> q.getQuestionType() == QuizQuestionType.ESSAY)
                .forEach(q -> {
                    Double assignedScore = scoreMap.get(q.getQuestionId());
                    if (assignedScore == null) {
                        throw new BadRequestException(ErrorCode.QUIZ_ESSAY_SCORE_INVALID, "Missing score for essay question: " + q.getQuestionId());
                    }
                    q.setScore(assignedScore / 10.0);
                });

        quizSessionQuestionRepository.saveAll(questions);

        List<QuizSessionQuestion> allQuestions = quizSessionQuestionRepository
                .findAllByQuizSessionId(sessionId, PageQuery.unpaged())
                .content();

        double totalFraction = allQuestions.stream()
                .mapToDouble(q -> q.getScore() != null ? q.getScore() : 0.0)
                .sum();
        double rawScore = allQuestions.isEmpty() ? 0.0 : (totalFraction / allQuestions.size()) * 10.0;
        int correctCount = (int) allQuestions.stream()
                .filter(q -> q.getScore() != null && q.getScore() >= 1.0)
                .count();

        session.setStatus(QuizSessionStatus.GRADED);
        quizSessionRepository.save(session);

        QuizSessionSubmission submission = quizSessionSubmissionRepository
                .findByQuizSessionId(sessionId)
                .orElseThrow(() -> new BadRequestException("Quiz session submission not found"));
        submission.setScore(rawScore);
        submission.setTotalCorrectAnswers(correctCount);
        quizSessionSubmissionRepository.save(submission);

        double effectiveScore = quizScoreAggregator.aggregateScore(session.getCourseId(), session.getUserId(), session.getQuizLessonId());
        log.info("Essay graded for session {}. rawScore={} effectiveScore={}", sessionId, rawScore, effectiveScore);

        boolean isPassed = effectiveScore >= PASSING_SCORE;

        LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                session.getUserId(),
                session.getQuizLessonId(),
                new UpdateLearningItemCommand(true, isPassed, effectiveScore));

        if (result.passedTransition()) {
            dailyGoalService.recordProgress(session.getUserId(), DailyGoalType.QUIZZES_PASSED, 1);
        }

        return toView(session);
    }

    @Override
    public List<QuizQuestionAnalyticsView> getQuizAnalytics(String courseId, String lessonId) {
        courseService.getQuizLessonById(courseId, lessonId);
        return quizSessionQuestionRepository.getQuizAnalyticsByQuizId(lessonId)
                .stream()
                .map(obj -> new QuizQuestionAnalyticsView(
                        (String) obj[0],
                        ((Number) obj[1]).intValue(),
                        ((Number) obj[2]).intValue(),
                        ((Number) obj[1]).doubleValue() == 0 ? 0.0 : ((Number) obj[2]).doubleValue() / ((Number) obj[1]).doubleValue()
                ))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRateString = "${app.quiz.auto-submit.rate:60000}")
    @Transactional
    public void autoSubmitExpiredSessions() {
        List<QuizSession> expiredSessions = quizSessionRepository.findExpiredSessions(QuizSessionStatus.IN_PROGRESS, LocalDateTime.now());
        if (!expiredSessions.isEmpty()) {
            log.info("Found {} expired quiz sessions. Starting auto-submission.", expiredSessions.size());
        }
        for (QuizSession session : expiredSessions) {
            try {
                // Submit empty request, the internal logic will use currently saved answers since it's already expired
                submitQuizSession(session.getId(), new SubmitQuizSessionCommand(Collections.emptyList()));
                log.info("Auto-submitted expired quiz session: {}", session.getId());
            } catch (Exception e) {
                log.error("Failed to auto-submit expired quiz session: {}", session.getId(), e);
            }
        }
    }

    private boolean isReviewStatus(QuizSessionStatus status) {
        return status == QuizSessionStatus.SUBMITTED || status == QuizSessionStatus.GRADED;
    }

    private Map<String, ChooseQuizAnswerCommand> buildAnswerMap(SubmitQuizSessionCommand request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ChooseQuizAnswerCommand> answerMap = new HashMap<>();
        for (ChooseQuizAnswerCommand answer : request.answers()) {
            if (answer == null) {
                throw new BadRequestException(ErrorCode.QUIZ_ANSWER_INVALID, "answers must not contain null items");
            }
            if (answerMap.putIfAbsent(answer.questionId(), answer) != null) {
                throw new BadRequestException(ErrorCode.QUIZ_ANSWER_INVALID, "Duplicate answer for questionId: " + answer.questionId());
            }
        }
        return answerMap;
    }

    private Map<String, Double> buildEssayScoreMap(GradeEssayCommand request) {
        if (request == null || request.scores() == null || request.scores().isEmpty()) {
            throw new BadRequestException(ErrorCode.QUIZ_ESSAY_SCORE_INVALID, "Essay scores are required");
        }
        Map<String, Double> scoreMap = new HashMap<>();
        for (GradeEssayCommand.EssayQuestionScore score : request.scores()) {
            if (score == null) {
                throw new BadRequestException(ErrorCode.QUIZ_ESSAY_SCORE_INVALID, "Essay scores must not contain null items");
            }
            if (scoreMap.putIfAbsent(score.questionId(), score.score()) != null) {
                throw new BadRequestException(ErrorCode.QUIZ_ESSAY_SCORE_INVALID, "Duplicate score for essay question: " + score.questionId());
            }
        }
        return scoreMap;
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1) {
            throw new BadRequestException(ErrorCode.INVALID_PAGINATION, "page must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new BadRequestException(ErrorCode.INVALID_PAGINATION, "size must be greater than or equal to 1");
        }
    }
}
