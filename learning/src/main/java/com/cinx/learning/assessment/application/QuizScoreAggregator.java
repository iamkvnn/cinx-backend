package com.cinx.learning.assessment.application;

import com.cinx.learning.shared.domain.ScoringMode;
import com.cinx.learning.assessment.domain.QuizSessionSubmissionRepository;
import com.cinx.learning.courseview.application.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuizScoreAggregator {
    private final QuizSessionSubmissionRepository quizSessionSubmissionRepository;
    private final CourseService courseService;

    public double aggregateScore(String courseId, String userId, String quizLessonId) {

        ScoringMode scoringMode = null;
        try {
            scoringMode = courseService.getQuizLessonById(courseId, quizLessonId).data().scoringMode();
        } catch (Exception e) {
            log.warn("Could not fetch scoringMode for quizLessonId={}, defaulting to HIGHEST", quizLessonId);
        }
        if (scoringMode == null)
            scoringMode = ScoringMode.HIGHEST;

        return aggregateScore(userId, quizLessonId, scoringMode);
    }

    public double aggregateScore(String userId, String quizLessonId, ScoringMode scoringMode) {
        return aggregateScore(
                quizSessionSubmissionRepository.findScoresByUserIdAndQuizLessonId(userId, quizLessonId),
                scoringMode);
    }

    public double aggregateScore(List<Double> scores, ScoringMode scoringMode) {
        List<Double> validScores = scores == null
                ? List.of()
                : scores.stream().filter(Objects::nonNull).toList();
        if (validScores.isEmpty()) {
            return 0.0;
        }
        return switch (scoringMode) {
            case HIGHEST -> validScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case LATEST -> validScores.getLast();
            case FIRST -> validScores.getFirst();
            case AVERAGE -> validScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        };
    }
}
