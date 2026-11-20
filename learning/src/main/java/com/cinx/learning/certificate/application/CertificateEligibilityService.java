package com.cinx.learning.certificate.application;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.certificate.domain.CourseCertificateConditions;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.certificate.domain.CourseCertificateConditionsRepository;
import com.cinx.learning.certificate.application.query.CertificateRequestView.CertificateChecks;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks a learner against a course's certificate conditions: the share of lessons completed, the lowest quiz score
 * and the lowest assignment score (both on the 0–10 item scale; a quiz not taken or an assignment not graded counts
 * as 0). A course without conditions uses the defaults, which match the old "every item completed and passed" rule
 * for assignments.
 */
@Component
@RequiredArgsConstructor
public class CertificateEligibilityService {
    private final CatalogLookup catalogLookup;
    private final ProgressLookup progressLookup;
    private final CourseCertificateConditionsRepository conditionsRepository;

    @Transactional(readOnly = true)
    public CourseCertificateConditions conditionsOf(String courseId) {
        return conditionsRepository.findByCourseId(courseId).orElseGet(() -> CourseCertificateConditions.defaults(courseId));
    }

    /** The course's readable lessons, loaded once per course when several learners are checked. */
    @Transactional(readOnly = true)
    public List<LessonInfo> lessonsOf(String courseId) {
        return catalogLookup.readableLessons(courseId);
    }

    @Transactional(readOnly = true)
    public CertificateChecks evaluate(String userId, String courseId, CourseCertificateConditions conditions, List<LessonInfo> lessons) {
        Map<String, LearningItemProgressInfo> items = progressLookup.itemProgressOfUser(userId, courseId).stream()
                .collect(Collectors.toMap(LearningItemProgressInfo::getItemId, item -> item, (a, b) -> a));
        return check(lessons, items, conditions);
    }

    static CertificateChecks check(List<LessonInfo> lessons, Map<String, LearningItemProgressInfo> items, CourseCertificateConditions conditions) {
        int total = lessons.size();
        int done = 0;
        Double minQuiz = null;
        String failingQuiz = null;
        Double minAssignment = null;
        String failingAssignment = null;
        for (LessonInfo lesson : lessons) {
            LearningItemProgressInfo item = items.get(lesson.getStableId());
            if (item != null && Boolean.TRUE.equals(item.getIsCompleted())) {
                done++;
            }
            double score = item == null || item.getScore() == null ? 0.0 : item.getScore();
            if (lesson.getLessonType() == LessonType.QUIZ && (minQuiz == null || score < minQuiz)) {
                minQuiz = score;
                failingQuiz = score < conditions.getMinQuizScore() ? lesson.getTitle() : failingQuiz;
            } else if (lesson.getLessonType() == LessonType.ASSIGNMENT && (minAssignment == null || score < minAssignment)) {
                minAssignment = score;
                failingAssignment = score < conditions.getMinAssignmentScore() ? lesson.getTitle() : failingAssignment;
            }
        }
        int percent = total == 0 ? 100 : (int) Math.floor(done * 100.0 / total);
        boolean eligible = percent >= conditions.getMinCompletionPercent()
                && (minQuiz == null || minQuiz >= conditions.getMinQuizScore())
                && (minAssignment == null || minAssignment >= conditions.getMinAssignmentScore());
        return new CertificateChecks(percent, done, total, minQuiz, failingQuiz, minAssignment, failingAssignment, eligible);
    }
}
