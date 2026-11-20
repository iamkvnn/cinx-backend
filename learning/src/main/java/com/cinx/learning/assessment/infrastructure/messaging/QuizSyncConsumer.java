package com.cinx.learning.assessment.infrastructure.messaging;

import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import com.cinx.learning.shared.domain.ScoringMode;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.courseview.application.query.QuizQuestionView;
import com.cinx.learning.assessment.infrastructure.messaging.event.QuizSyncEvent;
import com.cinx.learning.assessment.domain.QuizScoreHistory;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import com.cinx.learning.assessment.domain.QuizScoreHistoryRepository;
import com.cinx.learning.assessment.domain.QuizSessionQuestionRepository;
import com.cinx.learning.assessment.domain.QuizSessionSubmissionRepository;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.progress.application.ILearningProgressService;
import com.cinx.learning.progress.application.LearningItemProgressUpdateResult;
import com.cinx.learning.assessment.application.QuizScoreAggregator;
import com.cinx.learning.assessment.application.QuizSnapshotBuilder;
import com.cinx.learning.assessment.application.evaluator.IQuestionEvaluator;
import com.cinx.learning.assessment.application.evaluator.QuestionEvaluatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizSyncConsumer {
    private static final double PASSING_SCORE = 5.0;

    private final QuizScoreAggregator quizScoreAggregator;
    private final QuizSnapshotBuilder snapshotBuilder;
    private final QuestionEvaluatorFactory questionEvaluatorFactory;
    private final QuizSessionQuestionRepository quizSessionQuestionRepository;
    private final QuizSessionSubmissionRepository quizSessionSubmissionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizScoreHistoryRepository quizScoreHistoryRepository;
    private final ILearningProgressService learningProgressService;
    private final IDailyGoalService dailyGoalService;

    @RabbitListener(queues = "learning.sync-and-regrade.queue", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void receiveQuizSyncEvent(QuizSyncEvent event) {
        log.info("Received QuizSyncEvent: quizLessonId={}, changedQuestions={}",
                event.getQuizLessonId(), event.getQuestions().size());

        Map<String, QuizQuestionView> incomingMap = event.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestionView::id, q -> q));

        List<String> affectedQuestionIds = new ArrayList<>(incomingMap.keySet());

        List<QuizSessionQuestion> affectedRows = quizSessionQuestionRepository
                .findAllByQuizLessonIdAndQuestionIdIn(event.getQuizLessonId(), affectedQuestionIds);

        if (affectedRows.isEmpty()) {
            log.info("No QuizSessionQuestion rows found for affected questions in quizLessonId={}",
                    event.getQuizLessonId());
            return;
        }

        List<QuizSessionQuestion> needRegrade = new ArrayList<>();

        for (QuizSessionQuestion row : affectedRows) {
            QuizQuestionView incoming = incomingMap.get(row.getQuestionId());
            if (incoming == null) continue;

            String newCorrectAnswer = snapshotBuilder.buildCorrectAnswer(incoming);
            String newOptionsSnapshot = snapshotBuilder.buildOptionsSnapshot(incoming, false);

            boolean scoringAffected = false;

            if (!Objects.equals(row.getCorrectAnswer(), newCorrectAnswer)) {
                row.setCorrectAnswer(newCorrectAnswer);
                scoringAffected = true;
            }
            if (incoming.scoringMethod() != null
                    && !Objects.equals(row.getScoringMethod(), incoming.scoringMethod())) {
                row.setScoringMethod(incoming.scoringMethod());
                scoringAffected = true;
            }

            row.setQuestionText(incoming.questionText());
            row.setOptionsSnapshot(newOptionsSnapshot);

            if (scoringAffected) {
                needRegrade.add(row);
            }
        }

        quizSessionQuestionRepository.saveAll(affectedRows);
        log.info("Updated {} QuizSessionQuestion rows; {} need regrade", affectedRows.size(), needRegrade.size());

        if (needRegrade.isEmpty()) {
            log.info("No scoring impact detected — snapshot updated, no regrade needed.");
            return;
        }

        Map<String, QuizSession> affectedSessions = quizSessionRepository
                .findAllById(needRegrade.stream().map(QuizSessionQuestion::getQuizSessionId).filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(QuizSession::getId, s -> s, (a, b) -> a));
        Map<String, QuizSessionSubmission> submissionMap = quizSessionSubmissionRepository
                .findAllByQuizSessionIdIn(affectedSessions.keySet())
                .stream()
                .collect(Collectors.toMap(QuizSessionSubmission::getQuizSessionId, s -> s));

        Map<String, List<QuizSessionQuestion>> questionsMap = quizSessionQuestionRepository
                .findAllByQuizSessionIdIn(affectedSessions.keySet())
                .stream()
                .collect(Collectors.groupingBy(QuizSessionQuestion::getQuizSessionId));

        int regradeCount = 0;

        for (QuizSession session : affectedSessions.values()) {
            QuizSessionStatus status = session.getStatus();

            if (status == QuizSessionStatus.IN_PROGRESS) {
                log.debug("Session {} is IN_PROGRESS — snapshot updated, will be graded on submit.", session.getId());
                continue;
            }

            List<QuizSessionQuestion> allSessionQuestions = questionsMap.getOrDefault(session.getId(), Collections.emptyList());

            double totalFraction = 0.0;
            int correctCount = 0;
            for (QuizSessionQuestion q : allSessionQuestions) {
                if (q.getQuestionType() == QuizQuestionType.ESSAY) {
                    // PENDING_GRADE: keep existing essay score, non-essay will be re-evaluated
                    totalFraction += q.getScore() != null ? q.getScore() : 0.0;
                    continue;
                }
                IQuestionEvaluator evaluator = questionEvaluatorFactory.resolve(q);
                double fraction = evaluator.evaluate(q);
                q.setScore(fraction);
                if (fraction >= 1.0) correctCount++;
                totalFraction += fraction;
            }
            quizSessionQuestionRepository.saveAll(allSessionQuestions);

            double newRawScore = allSessionQuestions.isEmpty() ? 0.0 : (totalFraction / allSessionQuestions.size()) * 10.0;

            QuizSessionSubmission submission = submissionMap.get(session.getId());

            double oldScore = submission.getScore() != null ? submission.getScore() : 0.0;
            submission.setScore(newRawScore);
            submission.setTotalCorrectAnswers(correctCount);
            quizSessionSubmissionRepository.save(submission);

            quizScoreHistoryRepository.save(QuizScoreHistory.builder()
                    .quizSessionId(session.getId())
                    .oldScore(oldScore)
                    .newScore(newRawScore)
                    .reason(event.getChangeReason())
                    .gradedAt(LocalDateTime.now())
                    .build());
            regradeCount++;
            log.info("Regraded session {} | old={} new={}", session.getId(), oldScore, newRawScore);
            if (status != QuizSessionStatus.PENDING_GRADE) {
                updateLearningProgress(session.getUserId(), session.getQuizLessonId(), event.getScoringMode());
            }
        }

        log.info("Regrade complete for quizLessonId={}. {} sessions had score changes.",
                event.getQuizLessonId(), regradeCount);
    }

    private void updateLearningProgress(String userId, String quizLessonId, ScoringMode scoringMode) {
        double effectiveScore = quizScoreAggregator.aggregateScore(userId, quizLessonId, scoringMode);

        LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                userId,
                quizLessonId,
                new UpdateLearningItemCommand(true, effectiveScore >= PASSING_SCORE, effectiveScore)
        );
        if (result.passedTransition()) {
            dailyGoalService.recordProgress(userId, DailyGoalType.QUIZZES_PASSED, 1);
        }

        log.info("Updated learning progress for userId={} quizLessonId={} effectiveScore={}",
                userId, quizLessonId, effectiveScore);
    }
}
