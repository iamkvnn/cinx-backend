package com.cinx.learning.instructor.application;

import com.cinx.learning.community.application.CommunityLookup;

import com.cinx.learning.certificate.application.CertificateLookup;

import com.cinx.learning.assessment.application.AssessmentLookup;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.query.InboxView;
import com.cinx.learning.instructor.application.query.InboxView.Group;
import com.cinx.learning.instructor.application.query.InboxView.SourceStatus;
import com.cinx.learning.instructor.application.query.InboxView.Urgency;
import com.cinx.learning.instructor.domain.InstructorActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Builds the inbox from each source separately. A source that fails is reported FAILED and the others are still
 * shown, which is why this service is not one transaction: each repository call runs on its own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorInboxService implements IInstructorInboxService {
    private final CommunityLookup communityLookup;
    private final CertificateLookup certificateLookup;
    private final CatalogLookup catalogLookup;
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final Duration DUE_SOON = Duration.ofHours(24);
    static final Duration OVERDUE = Duration.ofHours(48);

    private final InstructorCourseLookup courseLookup;
    private final AssessmentLookup assessmentLookup;
    private final InstructorActivityRepository activityRepository;
    private final Clock clock;

    @Override
    public InboxView inbox(String instructorId, String courseId, InboxKind kind, String query) {
        List<InstructorCourseLookup.CourseCard> managed = courseLookup.managedCourses(instructorId);
        Map<String, InstructorCourseLookup.CourseCard> courses = managed.stream()
                .collect(Collectors.toMap(InstructorCourseLookup.CourseCard::id, Function.identity(), (a, b) -> a));
        List<String> courseIds = List.copyOf(courses.keySet());
        Instant now = clock.instant();
        LocalDateTime startOfToday = startOfToday(now);

        List<Group> all = new ArrayList<>();
        Map<String, InstructorCourseLookup.LessonRef> lessons = courseIds.isEmpty() ? Map.of() : courseLookup.lessonsOfCourses(courseIds);
        SourceStatus grading = collect(all, () -> grading(courses, lessons, now, startOfToday));
        SourceStatus questions = collect(all, () -> questions(courses, lessons, courseIds, now));
        SourceStatus reviews = collect(all, () -> reviews(courses, courseIds, now));
        SourceStatus certificates = collect(all, () -> certificates(courses, courseIds, now));
        SourceStatus quizEssays = collect(all, () -> quizEssays(courses, lessons, courseIds, now));
        SourceStatus courseRejected = collect(all, () -> rejected(managed, now));

        Map<String, Long> waitingPerCourse = all.stream().collect(Collectors.groupingBy(Group::courseId, Collectors.summingLong(Group::count)));
        List<InboxView.CourseWork> courseWork = managed.stream()
                .map(c -> new InboxView.CourseWork(c.id(), c.title(), c.thumbnailUrl(), waitingPerCourse.getOrDefault(c.id(), 0L)))
                .sorted(Comparator.comparingLong(InboxView.CourseWork::waiting).reversed().thenComparing(InboxView.CourseWork::title,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        String course = courseId == null || courseId.isBlank() || "all".equals(courseId) ? null : courseId;
        String q = query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
        List<Group> groups = all.stream()
                .filter(g -> course == null || g.courseId().equals(course))
                .filter(g -> kind == null || g.kind() == kind)
                .filter(g -> q == null || contains(g.title(), q) || contains(g.courseTitle(), q))
                .sorted(Comparator.comparing((Group g) -> g.urgency().ordinal()).thenComparing(Group::oldestAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        long doneToday = safe(() -> activityRepository.sumItemsSince(instructorId, startOfToday), 0L);
        return new InboxView(new InboxView.Sources(grading, questions, reviews, certificates, quizEssays, courseRejected),
                courseWork, groups, doneToday);
    }

    @Override
    public InboxView.Summary summary(String instructorId) {
        InboxView inbox = inbox(instructorId, null, null, null);
        long waiting = inbox.groups().stream().mapToLong(Group::count).sum();
        long overdue = inbox.groups().stream().filter(g -> g.urgency() == Urgency.OVERDUE).mapToLong(Group::count).sum();
        long pendingCertificates = inbox.groups().stream().filter(g -> g.kind() == InboxKind.CERTIFICATES).mapToLong(Group::count).sum();
        return new InboxView.Summary(waiting, overdue, pendingCertificates, inbox.courses().size());
    }

    private List<Group> grading(Map<String, InstructorCourseLookup.CourseCard> courses, Map<String, InstructorCourseLookup.LessonRef> lessons,
                                Instant now, LocalDateTime startOfToday) {
        List<String> assignments = lessons.values().stream()
                .filter(l -> l.type() == LessonType.ASSIGNMENT)
                .map(InstructorCourseLookup.LessonRef::stableId)
                .toList();
        if (assignments.isEmpty()) {
            return List.of();
        }
        return assessmentLookup.ungradedAssignmentGroups(assignments).stream()
                .map(row -> {
                    InstructorCourseLookup.LessonRef lesson = lessons.get(row.getAssignmentId());
                    Instant oldest = legacy(row.getOldest());
                    return group(InboxKind.GRADING, lesson.courseId(), courses, row.getAssignmentId(), lesson.title(),
                            row.getWaiting(), 0, 0, oldest, now, 3.0,
                            assessmentLookup.gradedAssignmentSubmissionsSince(row.getAssignmentId(), startOfToday));
                })
                .toList();
    }

    private List<Group> questions(Map<String, InstructorCourseLookup.CourseCard> courses, Map<String, InstructorCourseLookup.LessonRef> lessons,
                                  List<String> courseIds, Instant now) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return communityLookup.unansweredQuestionGroups(courseIds).stream()
                .map(row -> {
                    InstructorCourseLookup.LessonRef lesson = row.getLessonId() == null ? null : lessons.get(row.getLessonId());
                    String target = row.getLessonId() == null ? row.getCourseId() : row.getLessonId();
                    String title = lesson != null ? lesson.title() : courses.get(row.getCourseId()).title();
                    return group(InboxKind.QUESTIONS, row.getCourseId(), courses, target, title, row.getWaiting(), row.getUpvotes(), 0,
                            legacy(row.getOldest()), now, 2.0, 0);
                })
                .toList();
    }

    private List<Group> reviews(Map<String, InstructorCourseLookup.CourseCard> courses, List<String> courseIds, Instant now) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return communityLookup.unrepliedReviewGroups(courseIds).stream()
                .map(row -> group(InboxKind.REVIEWS, row.getCourseId(), courses, row.getCourseId(), courses.get(row.getCourseId()).title(),
                        row.getWaiting(), 0, 0, legacy(row.getOldest()), now, 1.5, 0))
                .toList();
    }

    private List<Group> certificates(Map<String, InstructorCourseLookup.CourseCard> courses, List<String> courseIds, Instant now) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return certificateLookup.pendingGroups(courseIds).stream()
                .map(row -> group(InboxKind.CERTIFICATES, row.getCourseId(), courses, row.getCourseId(), courses.get(row.getCourseId()).title(),
                        row.getWaiting(), 0, eligibleCount(row.getCourseId()), legacy(row.getOldest()), now, 0.5, 0))
                .toList();
    }

    private List<Group> quizEssays(Map<String, InstructorCourseLookup.CourseCard> courses, Map<String, InstructorCourseLookup.LessonRef> lessons,
                                   List<String> courseIds, Instant now) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return assessmentLookup.pendingGradeGroups(courseIds).stream()
                .map(row -> {
                    InstructorCourseLookup.LessonRef lesson = lessons.get(row.getQuizLessonId());
                    String title = lesson != null ? lesson.title() : courses.get(row.getCourseId()).title();
                    return group(InboxKind.QUIZ_ESSAYS, row.getCourseId(), courses, row.getQuizLessonId(), title, row.getWaiting(), 0, 0,
                            legacy(row.getOldest()), now, 3.0, 0);
                })
                .toList();
    }

    /** A course sent back by review is one piece of work: its notes are fixed in the course editor. */
    private List<Group> rejected(List<InstructorCourseLookup.CourseCard> managed, Instant now) {
        Map<String, InstructorCourseLookup.CourseCard> courses = managed.stream()
                .collect(Collectors.toMap(InstructorCourseLookup.CourseCard::id, Function.identity(), (a, b) -> a));
        List<Group> groups = new ArrayList<>();
        for (InstructorCourseLookup.CourseCard course : managed) {
            if (!CoursePublishStatus.REJECTED.name().equals(course.publishStatus())) {
                continue;
            }
            long notes = catalogLookup.sentReviewNoteCount(course.id());
            Instant rejectedAt = catalogLookup.lastRejectedAt(course.id())
                    .map(InstructorInboxService::legacy)
                    .orElse(now);
            groups.add(group(InboxKind.COURSE_REJECTED, course.id(), courses, course.id(), course.title(), Math.max(1, notes), 0, 0,
                    rejectedAt, now, 20.0, 0));
        }
        return groups;
    }

    private long eligibleCount(String courseId) {
        return certificateLookup.eligiblePendingCount(courseId);
    }

    private static Group group(InboxKind kind, String courseId, Map<String, InstructorCourseLookup.CourseCard> courses, String targetId,
                               String title, long count, long upvotes, long eligible, Instant oldest, Instant now,
                               double minutesPerItem, long progressDone) {
        InstructorCourseLookup.CourseCard course = courses.get(courseId);
        return new Group(
                kind.name() + ":" + courseId + ":" + targetId,
                kind,
                courseId,
                course == null ? null : course.title(),
                targetId,
                title,
                count,
                upvotes,
                eligible,
                oldest,
                urgency(oldest, now),
                (int) Math.ceil(count * minutesPerItem),
                progressDone);
    }

    static Urgency urgency(Instant oldest, Instant now) {
        if (oldest == null) {
            return Urgency.NEW;
        }
        Duration age = Duration.between(oldest, now);
        if (age.compareTo(OVERDUE) > 0) {
            return Urgency.OVERDUE;
        }
        return age.compareTo(DUE_SOON) >= 0 ? Urgency.DUE_SOON : Urgency.NEW;
    }

    private SourceStatus collect(List<Group> into, Supplier<List<Group>> source) {
        try {
            into.addAll(source.get());
            return SourceStatus.OK;
        } catch (RuntimeException e) {
            log.warn("An inbox source failed", e);
            return SourceStatus.FAILED;
        }
    }

    private static <T> T safe(Supplier<T> call, T fallback) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Start of today in the business zone, as the UTC wall time the clock-written columns use. */
    private static LocalDateTime startOfToday(Instant now) {
        Instant start = now.atZone(BUSINESS_ZONE).toLocalDate().atStartOfDay(BUSINESS_ZONE).toInstant();
        return LocalDateTime.ofInstant(start, ZoneOffset.UTC);
    }

    /** Columns written with LocalDateTime.now() or JPA auditing hold the JVM's wall time. */
    private static Instant legacy(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
