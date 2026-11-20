package com.cinx.learning.enrollment.application;

import com.cinx.learning.engagement.application.EngagementLookup;

import com.cinx.learning.certificate.application.CertificateLookup;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.learning.catalog.application.query.CourseImageView;
import com.cinx.learning.catalog.application.query.CourseView;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.CurriculumLookup;
import com.cinx.learning.catalog.application.query.SectionPlace;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.enrollment.application.query.MyCourseView;
import com.cinx.learning.certificate.application.query.CertificateRequestInfo;
import com.cinx.learning.enrollment.domain.CourseEntitlement;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.engagement.application.query.LearningActivityDailyInfo;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import com.cinx.learning.enrollment.domain.CourseEntitlementRepository;
import com.cinx.learning.studyplan.application.NextLessonResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyCoursesService implements IMyCoursesService {
    private final EngagementLookup engagementLookup;
    private final CertificateLookup certificateLookup;
    private final CatalogLookup catalogLookup;
    private final ProgressLookup progressLookup;
    private final CourseEntitlementRepository entitlementRepository;
    private final ICourseService courseService;
    private final CurriculumLookup curriculum;
    private final NextLessonResolver nextLessonResolver;

    @Override
    @Transactional(readOnly = true)
    public List<MyCourseView> myCourses(String userId) {
        Map<String, LocalDateTime> grantedAt = new LinkedHashMap<>();
        for (CourseEntitlement entitlement : entitlementRepository.findAllByUserIdOrderByGrantedAtDesc(userId)) {
            grantedAt.putIfAbsent(entitlement.getCourseId(), entitlement.getGrantedAt());
        }
        if (grantedAt.isEmpty()) {
            return List.of();
        }
        List<String> courseIds = new ArrayList<>(grantedAt.keySet());

        Map<String, CourseView> courses = courseService.getReadableCourseByIds(userId, courseIds).stream()
                .collect(Collectors.toMap(CourseView::id, Function.identity(), (a, b) -> a));
        Map<String, CourseProgressInfo> progress = progressLookup.courseProgressOfUser(userId, courseIds).stream()
                .collect(Collectors.toMap(CourseProgressInfo::getCourseId, Function.identity(), (a, b) -> a));
        Map<String, LearningActivityDailyInfo> latest = engagementLookup.latestActivityWithLessonPerCourse(userId, courseIds).stream()
                .collect(Collectors.toMap(LearningActivityDailyInfo::getCourseId, Function.identity(), (a, b) -> a));
        Map<String, CertificateRequestInfo> certificates = certificateLookup.requestsOfUser(userId).stream()
                .collect(Collectors.toMap(CertificateRequestInfo::getCourseId, Function.identity(), (a, b) -> a));
        // readable lessons of all courses in one query, kept in course order per course
        List<LessonInfo> readableLessons = catalogLookup.readableLessonsOfCourses(courseIds);
        Map<String, SectionPlace> places = curriculum.sectionPlaces(
                readableLessons.stream().map(LessonInfo::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<String, List<LessonInfo>> lessons = readableLessons.stream()
                .collect(Collectors.groupingBy(lesson -> places.get(lesson.getSectionId()).courseId(), LinkedHashMap::new, Collectors.toList()));
        List<String> lessonIds = lessons.values().stream().flatMap(List::stream).map(LessonInfo::getStableId).toList();
        Set<String> completed = lessonIds.isEmpty() ? Set.of() : progressLookup.itemProgressOfUser(userId, lessonIds).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                .map(LearningItemProgressInfo::getItemId)
                .collect(Collectors.toSet());

        return courseIds.stream()
                .filter(courses::containsKey)
                .map(courseId -> toResponse(userId, courses.get(courseId), grantedAt.get(courseId), progress.get(courseId),
                        latest.get(courseId), lessons.getOrDefault(courseId, List.of()), completed, certificates.get(courseId)))
                .sorted(Comparator.comparing(MyCourseView::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MyCourseView::grantedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private MyCourseView toResponse(String userId, CourseView course, LocalDateTime grantedAt, CourseProgressInfo progress,
                                        LearningActivityDailyInfo activity, List<LessonInfo> lessons, Set<String> completed,
                                        CertificateRequestInfo certificate) {
        // without a progress row yet, count from the readable curriculum; nothing is written on read
        int total = progress != null && progress.getTotalItems() != null ? progress.getTotalItems() : lessons.size();
        int done = progress != null && progress.getCompletedItems() != null
                ? progress.getCompletedItems()
                : (int) lessons.stream().filter(lesson -> completed.contains(lesson.getStableId())).count();
        boolean finished = progress != null && Boolean.TRUE.equals(progress.getIsCompleted());
        MyCourseView.Next next = finished || lessons.isEmpty() ? null : next(userId, lessons, completed, activity);
        return new MyCourseView(
                course.id(),
                course.title(),
                thumbnail(course),
                course.category() == null ? null : new MyCourseView.Category(course.category().id(), course.category().name()),
                course.instructor() == null ? null : new MyCourseView.Instructor(course.instructor().id(), course.instructor().name()),
                grantedAt,
                total,
                done,
                finished,
                progress == null ? null : progress.getIsPassed(),
                progress == null ? null : progress.getAvgScore(),
                activity == null ? null : activity.getLastActivityAt(),
                next,
                Boolean.TRUE.equals(course.hasCertificate()),
                certificate == null ? null : new MyCourseView.Certificate(certificate.getId(), certificate.getStatus(),
                        certificate.getCertificateUrl(), certificate.getRequestedAt(), certificate.getApprovedAt(),
                        certificate.getRejectedAt(), certificate.getRejectionReason()));
    }

    private MyCourseView.Next next(String userId, List<LessonInfo> lessons, Set<String> completed, LearningActivityDailyInfo activity) {
        NextLessonResolver.Next next = nextLessonResolver.resolve(userId, lessons, completed, activity == null ? null : activity.getLastItemId());
        LessonInfo lesson = next.lesson();
        return new MyCourseView.Next(lesson.getStableId(), lesson.getTitle(), lesson.getLessonType(),
                next.sectionPosition(), next.lessonPosition(), next.positionSeconds(), next.remainingMinutes());
    }

    private static String thumbnail(CourseView course) {
        return course.images() == null ? null : course.images().stream()
                .map(CourseImageView::imageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
