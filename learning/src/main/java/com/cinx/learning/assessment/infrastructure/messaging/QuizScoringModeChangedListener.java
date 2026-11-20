package com.cinx.learning.assessment.infrastructure.messaging;

import java.util.function.Function;
import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.assessment.infrastructure.messaging.event.ScoringModeChangedEvent;
import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import com.cinx.learning.assessment.domain.QuizSessionSubmissionRepository;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.progress.application.ILearningProgressService;
import com.cinx.learning.progress.application.LearningItemProgressUpdateResult;
import com.cinx.learning.assessment.application.QuizScoreAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScoringModeChangedListener {
    private static final double PASSING_SCORE = 5.0;

    private final QuizSessionSubmissionRepository quizSessionSubmissionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ILearningProgressService learningProgressService;
    private final QuizScoreAggregator quizScoreAggregator;
    private final IDailyGoalService dailyGoalService;

    @Transactional
    @RabbitListener(queues = "learning.quiz.scoring-mode-change.queue", containerFactory = "rabbitListenerContainerFactory")
    public void onScoringModeChanged(ScoringModeChangedEvent event) {
        log.info("Received ScoringModeChangedEvent: quizLessonId={} newScoringMode={}",
                event.quizLessonId(), event.scoringMode());
        List<QuizSessionSubmission> allSubmissions = quizSessionSubmissionRepository.findAllByQuizLessonId(event.quizLessonId());
        Map<String, QuizSession> sessions = quizSessionRepository
                .findAllById(allSubmissions.stream().map(QuizSessionSubmission::getQuizSessionId).distinct().toList()).stream()
                .collect(Collectors.toMap(QuizSession::getId, Function.identity()));
        Map<String, List<QuizSessionSubmission>> submissionsByUser = allSubmissions.stream()
                .collect(Collectors.groupingBy(s -> sessions.get(s.getQuizSessionId()).getUserId()));


        for (Map.Entry<String, List<QuizSessionSubmission>> entry : submissionsByUser.entrySet()) {
            String userId = entry.getKey();
            List<QuizSessionSubmission> submissions = entry.getValue();

            List<Double> scores = submissions.stream()
                    .map(QuizSessionSubmission::getScore)
                    .toList();

            double newAggregatedScore = quizScoreAggregator.aggregateScore(scores, event.scoringMode());

            LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                    userId,
                     event.quizLessonId(),
                    new UpdateLearningItemCommand(true, newAggregatedScore >= PASSING_SCORE, newAggregatedScore)
            );
            if (result.passedTransition()) {
                dailyGoalService.recordProgress(userId, DailyGoalType.QUIZZES_PASSED, 1);
            }
        }
    }
}
