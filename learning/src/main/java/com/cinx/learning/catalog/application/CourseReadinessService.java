package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.LessonInfo;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.CourseReadiness;
import com.cinx.learning.catalog.application.query.CourseReadiness.Problem;
import com.cinx.learning.catalog.application.query.CourseReadiness.ProblemCode;
import com.cinx.learning.catalog.domain.ArticleLesson;
import com.cinx.learning.catalog.domain.AssignmentLesson;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.ArticleLessonRepository;
import com.cinx.learning.catalog.domain.AssignmentLessonRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseReadinessService implements ICourseReadinessService {
    private final ICourseDraftService courseDraftService;
    private final LessonRepository lessonRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final ArticleLessonRepository articleLessonRepository;
    private final AssignmentLessonRepository assignmentLessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    @Override
    @Transactional(readOnly = true)
    public CourseReadiness check(Course course) {
        Optional<CourseDraft> draft = courseDraftService.findDraft(course);
        String description = draft.map(CourseDraft::getDescription).orElse(course.getDescription());
        BigDecimal price = draft.map(CourseDraft::getPrice).orElse(course.getPrice());
        BigDecimal discountedPrice = draft.map(CourseDraft::getDiscountedPrice).orElse(course.getDiscountedPrice());
        List<Lesson> lessons = draft.map(d -> lessonRepository.findDraftByDraft(d.getId()))
                .orElseGet(() -> lessonRepository.findPublishedByCourse(course.getId()));

        List<Problem> problems = new ArrayList<>();
        if (description == null || description.isBlank()) {
            problems.add(Problem.course(ProblemCode.DESCRIPTION_MISSING));
        }
        if (course.getImages().isEmpty()) {
            problems.add(Problem.course(ProblemCode.COVER_MISSING));
        }
        if (!priceValid(price, discountedPrice)) {
            problems.add(Problem.course(ProblemCode.PRICE_INVALID));
        }
        if (lessons.isEmpty()) {
            problems.add(Problem.course(ProblemCode.NO_LESSON));
        }
        Set<String> withContent = lessonsWithContent(lessons);
        lessons.stream()
                .filter(lesson -> !withContent.contains(lesson.getStableId()))
                .forEach(lesson -> problems.add(Problem.lesson(lesson.getStableId(), lesson.getTitle(), lesson.getLessonType())));

        return new CourseReadiness(problems.isEmpty(), lessons.size(), withContent.size(), List.copyOf(problems));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> lessonsWithContent(Collection<Lesson> lessons) {
        return withContent(lessons.stream().filter(l -> l.getLessonType() != null)
                .collect(Collectors.groupingBy(Lesson::getLessonType, Collectors.mapping(Lesson::getStableId, Collectors.toList()))));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> lessonsWithContentOf(Collection<LessonInfo> lessons) {
        return withContent(lessons.stream().filter(l -> l.getLessonType() != null)
                .collect(Collectors.groupingBy(LessonInfo::getLessonType, Collectors.mapping(LessonInfo::getStableId, Collectors.toList()))));
    }

    private Set<String> withContent(Map<LessonType, List<String>> byType) {
        Set<String> ready = new HashSet<>();
        List<String> videos = byType.getOrDefault(LessonType.VIDEO, List.of());
        if (!videos.isEmpty()) {
            videoLessonRepository.findAllByLessonIdIn(videos).stream()
                    .filter((VideoLesson v) -> present(v.getFileKey()))
                    .forEach(v -> ready.add(v.getLessonId()));
        }
        List<String> articles = byType.getOrDefault(LessonType.ARTICLE, List.of());
        if (!articles.isEmpty()) {
            articleLessonRepository.findAllByLessonIdIn(articles).stream()
                    .filter((ArticleLesson a) -> present(a.getArticleUrl()) || present(a.getFileKey()))
                    .forEach(a -> ready.add(a.getLessonId()));
        }
        List<String> quizzes = byType.getOrDefault(LessonType.QUIZ, List.of());
        if (!quizzes.isEmpty()) {
            quizQuestionRepository.countPerQuizLesson(quizzes).stream()
                    .filter(q -> q.getTotal() > 0)
                    .forEach(q -> ready.add(q.getLessonId()));
        }
        List<String> assignments = byType.getOrDefault(LessonType.ASSIGNMENT, List.of());
        if (!assignments.isEmpty()) {
            assignmentLessonRepository.findAllByLessonIdIn(assignments).stream()
                    .filter((AssignmentLesson a) -> present(a.getDescription()))
                    .forEach(a -> ready.add(a.getLessonId()));
        }
        return ready;
    }

    /** cinx sells no free courses: the price is above zero, and a discount, when set, is below it. */
    private static boolean priceValid(BigDecimal price, BigDecimal discountedPrice) {
        if (price == null || price.signum() <= 0) {
            return false;
        }
        return discountedPrice == null || (discountedPrice.signum() > 0 && discountedPrice.compareTo(price) < 0);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
