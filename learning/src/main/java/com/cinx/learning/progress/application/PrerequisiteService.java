package com.cinx.learning.progress.application;

import com.cinx.learning.assessment.application.AssessmentLookup;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.ICourseAccessService;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import com.cinx.learning.progress.domain.LearningItemProgress;
import com.cinx.learning.progress.domain.LearningItemProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrerequisiteService implements IPrerequisiteService {
    private final CatalogLookup catalogLookup;
    private static final Set<QuizSessionStatus> QUIZ_SUBMITTED =
            Set.of(QuizSessionStatus.SUBMITTED, QuizSessionStatus.GRADED, QuizSessionStatus.PENDING_GRADE);
    private final LearningItemProgressRepository itemProgressRepository;
    private final AssessmentLookup assessmentLookup;
    private final ICourseAccessService courseAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<String> missing(String userId, String courseId, String lessonId) {
        List<LessonInfo> lessons = catalogLookup.readableLessons(courseId);
        LessonInfo lesson = lessons.stream().filter((l) -> l.getStableId().equals(lessonId)).findFirst().orElse(null);
        if (lesson == null || lesson.getPrerequisiteIds().isEmpty()) {
            return List.of();
        }
        Set<String> published = lessons.stream().map(LessonInfo::getStableId).collect(Collectors.toSet());
        Set<String> done = done(userId, lessons, Set.copyOf(lesson.getPrerequisiteIds()));
        return lesson.getPrerequisiteIds().stream().filter((id) -> published.contains(id) && !done.contains(id)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> locks(String userId, String courseId) {
        courseAccessService.ensureReadableCourse(userId, courseId);
        List<LessonInfo> lessons = catalogLookup.readableLessons(courseId);
        Set<String> wanted = lessons.stream().flatMap((l) -> l.getPrerequisiteIds().stream()).collect(Collectors.toSet());
        Map<String, List<String>> locks = new LinkedHashMap<>();
        if (wanted.isEmpty()) {
            return locks;
        }
        Set<String> published = lessons.stream().map(LessonInfo::getStableId).collect(Collectors.toSet());
        Set<String> done = done(userId, lessons, wanted);
        for (LessonInfo lesson : lessons) {
            // a prerequisite that is no longer published never locks anything
            List<String> missing = lesson.getPrerequisiteIds().stream().filter((id) -> published.contains(id) && !done.contains(id)).toList();
            if (!missing.isEmpty()) {
                locks.put(lesson.getStableId(), missing);
            }
        }
        return locks;
    }

    /** Which of the wanted prerequisite ids the learner has done: three batched reads, whatever the count. */
    private Set<String> done(String userId, List<LessonInfo> lessons, Set<String> wanted) {
        Map<String, LessonInfo> byId = lessons.stream().collect(Collectors.toMap(LessonInfo::getStableId, Function.identity(), (a, b) -> a));
        Set<String> done = new HashSet<>();
        itemProgressRepository.findAllByUserIdAndItemIdIn(userId, List.copyOf(wanted)).stream()
                .filter((item) -> Boolean.TRUE.equals(item.getIsCompleted()))
                .map(LearningItemProgress::getItemId)
                .forEach(done::add);
        List<String> quizzes = ofType(wanted, byId, LessonType.QUIZ, done);
        if (!quizzes.isEmpty()) {
            done.addAll(assessmentLookup.quizLessonIdsWithStatus(userId, quizzes, QUIZ_SUBMITTED));
        }
        List<String> assignments = ofType(wanted, byId, LessonType.ASSIGNMENT, done);
        if (!assignments.isEmpty()) {
            // assignments complete only at grading (AssignmentService.scoreAssignmentSubmission); submitting counts here
            done.addAll(assessmentLookup.submittedAssignmentIds(userId, assignments));
        }
        return done;
    }

    private static List<String> ofType(Set<String> wanted, Map<String, LessonInfo> byId, LessonType type, Set<String> done) {
        return wanted.stream()
                .filter((id) -> !done.contains(id))
                .filter((id) -> byId.containsKey(id) && byId.get(id).getLessonType() == type)
                .toList();
    }
}
