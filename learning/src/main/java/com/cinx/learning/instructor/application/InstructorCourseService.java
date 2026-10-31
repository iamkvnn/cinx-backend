package com.cinx.learning.instructor.application;

import com.cinx.learning.engagement.application.EngagementLookup;

import com.cinx.learning.community.application.CommunityLookup;

import com.cinx.learning.certificate.application.CertificateLookup;

import com.cinx.learning.assessment.application.AssessmentLookup;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.enrollment.application.EnrollmentAccess;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.learning.shared.application.UpstreamCallException;

import com.cinx.learning.assessment.application.query.QuizSessionSubmissionInfo;
import com.cinx.learning.catalog.application.query.SectionPlace;
import com.cinx.learning.catalog.application.CurriculumLookup;
import com.cinx.common.paging.PageResult;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedMetadata;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.UserView;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.application.query.CourseDraftInfo;
import com.cinx.learning.catalog.application.query.CourseInstructorMembershipInfo;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.ICourseAccessService;
import com.cinx.learning.catalog.application.ICourseReadinessService;
import com.cinx.learning.catalog.application.InstructorDirectory;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import com.cinx.learning.instructor.domain.CourseLifecycle;
import com.cinx.learning.instructor.application.query.CourseLearnersView;
import com.cinx.learning.instructor.application.query.InboxView;
import com.cinx.learning.instructor.application.query.InstructorCourseRow;
import com.cinx.learning.instructor.application.query.LessonReachView;
import com.cinx.learning.instructor.application.query.QuizSummary;
import com.cinx.learning.certificate.application.query.CertificateRequestInfo;
import com.cinx.learning.enrollment.application.query.CourseEntitlementInfo;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.assessment.application.query.QuizSessionInfo;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reads behind the instructor's course pages; the list is not transactional because the inbox sources fail on their own. */
@Service
@RequiredArgsConstructor
public class InstructorCourseService implements IInstructorCourseService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int NAME_BATCH = 100;
    private static final int INACTIVE_DAYS = 14;
    private static final int DESCRIPTION_LENGTH = 200;
    private final EngagementLookup engagementLookup;
    private final CommunityLookup communityLookup;
    private final CertificateLookup certificateLookup;
    private final CatalogLookup catalogLookup;
    private final EnrollmentAccess enrollmentAccess;
    private final ProgressLookup progressLookup;
    private final CurriculumLookup curriculum;
    private final AssessmentLookup assessmentLookup;
    private final IInstructorInboxService inboxService;
    private final ICourseAccessService courseAccessService;
    private final ICourseReadinessService readinessService;
    private final LearningAuthorizationService authorizationService;
    private final InstructorDirectory userService;
    private final Clock clock;

    // ---- the list and one row ------------------------------------------------------------------------------

    @Override
    public PageResult<InstructorCourseRow> courses(String userId, CourseQuery query) {
        requirePage(query.page(), query.size());
        List<CourseInfo> courses = catalogLookup.courses(catalogLookup.courseIdsOfInstructor(userId));
        Map<String, CourseMembershipRole> roles = roles(userId, courses);
        String needle = fold(query.q());
        List<CourseInfo> matching = courses.stream()
                .filter(c -> needle.isEmpty() || fold(c.getTitle()).contains(needle))
                .filter(c -> query.lifecycles() == null || query.lifecycles().isEmpty() || query.lifecycles().contains(CourseLifecycle.of(c.getStatus(), c.getPublishStatus())))
                .filter(c -> query.roles() == null || query.roles().isEmpty() || query.roles().contains(roles.get(c.getId())))
                .filter(c -> query.categoryId() == null || query.categoryId().equals(c.getCategoryId()))
                .toList();
        List<InstructorCourseRow> rows = rows(userId, matching, roles, inboxService.inbox(userId, null, null, null));
        List<InstructorCourseRow> sorted = rows.stream().sorted(courseOrder(query.sort(), query.ascending())).toList();
        int from = Math.min((query.page() - 1) * query.size(), sorted.size());
        int to = Math.min(from + query.size(), sorted.size());
        return new PageResult<>(sorted.subList(from, to), query.page(), query.size(), sorted.size());
    }

    @Override
    public InstructorCourseRow course(String userId, String courseId) {
        CourseInfo course = catalogLookup.course(courseId)
                .filter(c -> courseAccessService.hasCourseCapability(userId, c, CourseMembershipRole.EDITOR))
                .orElseThrow(() -> new NotFoundException("CourseInfo not found with id: " + courseId));
        Map<String, CourseMembershipRole> roles = roles(userId, List.of(course));
        InboxView inbox = roles.get(courseId) == CourseMembershipRole.EDITOR ? null : inboxService.inbox(userId, courseId, null, null);
        return rows(userId, List.of(course), roles, inbox).get(0);
    }

    /** The viewer's role per course: the active membership, else OWNER for the primary instructor (pre-V10 rows). */
    private Map<String, CourseMembershipRole> roles(String userId, List<CourseInfo> courses) {
        Map<String, CourseMembershipRole> byCourse = catalogLookup.activeMembershipsOf(userId)
                .stream().collect(Collectors.toMap(CourseInstructorMembershipInfo::getCourseId, CourseInstructorMembershipInfo::getRole, (a, b) -> a));
        Map<String, CourseMembershipRole> result = new HashMap<>();
        courses.forEach(course -> result.put(course.getId(), byCourse.getOrDefault(course.getId(),
                Objects.equals(course.getInstructorId(), userId) ? CourseMembershipRole.OWNER : CourseMembershipRole.EDITOR)));
        return result;
    }

    private List<InstructorCourseRow> rows(String userId, List<CourseInfo> courses, Map<String, CourseMembershipRole> roles, InboxView inbox) {
        if (courses.isEmpty()) {
            return List.of();
        }
        List<String> ids = courses.stream().map(CourseInfo::getId).toList();
        Map<String, Long> learners = enrollmentAccess.countPerCourse(ids).stream()
                .collect(Collectors.toMap(EnrollmentAccess.CourseCount::courseId, EnrollmentAccess.CourseCount::total));
        Map<String, long[]> work = new HashMap<>();
        if (inbox != null) {
            inbox.groups().forEach(group -> {
                long[] w = work.computeIfAbsent(group.courseId(), k -> new long[2]);
                w[0] += group.count();
                if (group.urgency() == InboxView.Urgency.OVERDUE) {
                    w[1] += group.count();
                }
            });
        }
        Map<String, String> ownerNames = names(courses.stream().map(CourseInfo::getInstructorId)
                .filter(owner -> !Objects.equals(owner, userId)).distinct().toList());
        Map<String, String> categoryNames = catalogLookup.categoryNames(courses.stream().map(CourseInfo::getCategoryId)
                .filter(Objects::nonNull).distinct().toList());
        return courses.stream().map(course -> {
            CourseMembershipRole role = roles.get(course.getId());
            boolean editor = role == CourseMembershipRole.EDITOR;
            CourseLifecycle lifecycle = CourseLifecycle.of(course.getStatus(), course.getPublishStatus());
            Optional<CourseDraftInfo> draft = catalogLookup.draftOfCourse(course.getId());
            List<LessonInfo> lessons = draft.map(d -> catalogLookup.draftLessons(d.getId()))
                    .orElseGet(() -> catalogLookup.publishedLessons(course.getId()));
            long duration = Optional.ofNullable(draft.map(CourseDraftInfo::getDuration).orElse(course.getDuration())).orElse(0L);
            long[] w = work.getOrDefault(course.getId(), new long[2]);
            int notes = lifecycle == CourseLifecycle.NEEDS_FIX
                    ? catalogLookup.sentReviewNoteCount(course.getId())
                    : 0;
            return new InstructorCourseRow(
                    course.getId(),
                    course.getTitle(),
                    shorten(draft.map(CourseDraftInfo::getDescription).orElse(course.getDescription())),
                    course.thumbnailUrl(),
                    categoryNames.get(course.getCategoryId()),
                    lifecycle,
                    course.getStatus() == CourseStatus.PUBLISHED && course.getPublishStatus() == null && draft.isPresent(),
                    role,
                    course.getInstructorId(),
                    Objects.equals(course.getInstructorId(), userId) ? null : ownerNames.get(course.getInstructorId()),
                    lessons.size(),
                    draft.isPresent() ? readinessService.lessonsWithContentOf(lessons).size() : lessons.size(),
                    duration,
                    course.getPublishedAt(),
                    course.getSubmittedAt(),
                    course.getArchivedAt(),
                    course.getUpdatedAt(),
                    lifecycleDate(course, lifecycle),
                    editor ? null : learners.getOrDefault(course.getId(), 0L),
                    editor ? null : w[0],
                    editor ? null : w[1],
                    notes,
                    course.getRating(),
                    course.getRatingCount() == null ? 0 : course.getRatingCount());
        }).toList();
    }

    private static LocalDateTime lifecycleDate(CourseInfo course, CourseLifecycle lifecycle) {
        return switch (lifecycle) {
            case LIVE -> course.getPublishedAt();
            // a returned update of a course on sale keeps its sale date; a returned first draft shows its last edit
            case NEEDS_FIX -> course.getStatus() == CourseStatus.PUBLISHED ? course.getPublishedAt() : course.getUpdatedAt();
            case IN_REVIEW -> course.getSubmittedAt();
            case DRAFT -> course.getUpdatedAt();
            case ARCHIVED -> course.getArchivedAt();
        };
    }

    private static Comparator<InstructorCourseRow> courseOrder(CourseSort sort, boolean ascending) {
        Comparator<InstructorCourseRow> order = switch (sort == null ? CourseSort.DATE : sort) {
            case DATE -> Comparator.comparing(InstructorCourseRow::lifecycleDate, Comparator.nullsFirst(Comparator.naturalOrder()));
            case LEARNERS -> Comparator.comparing(r -> r.learners() == null ? -1L : r.learners());
            case WAITING -> Comparator.comparing(r -> r.waiting() == null ? -1L : r.waiting());
            case TITLE -> Comparator.comparing(r -> fold(r.title()));
        };
        // descending by default; for dates the nulls ("—" on old courses) then sort last either way
        Comparator<InstructorCourseRow> directed = ascending ? order : order.reversed();
        return sort == null || sort == CourseSort.DATE
                ? Comparator.<InstructorCourseRow, Boolean>comparing(r -> r.lifecycleDate() == null).thenComparing(directed)
                : directed;
    }

    // ---- learners ------------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public CourseLearnersView learners(String userId, String courseId, LearnerQuery query) {
        authorizationService.requireCourseInstructor(userId, courseId);
        requirePage(query.page(), query.size());
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime inactiveBefore = now.minusDays(INACTIVE_DAYS);

        List<CourseEntitlementInfo> entitlements = enrollmentAccess.entitlementsOfCourse(courseId);
        Map<String, CourseProgressInfo> progress = progressLookup.courseProgressOfCourse(courseId).stream()
                .collect(Collectors.toMap(CourseProgressInfo::getUserId, Function.identity(), (a, b) -> a));
        Map<String, EngagementLookup.LearnerActivity> activity = engagementLookup.activityPerLearner(courseId).stream()
                .collect(Collectors.toMap(EngagementLookup.LearnerActivity::getUserId, Function.identity()));
        Map<String, CertificateRequestInfo> certificates = certificateLookup.requestsOfCourse(courseId).stream()
                .collect(Collectors.toMap(CertificateRequestInfo::getUserId, Function.identity(),
                        (a, b) -> Comparator.comparing(CertificateRequestInfo::getRequestedAt, Comparator.nullsFirst(Comparator.naturalOrder())).compare(a, b) >= 0 ? a : b));
        Map<String, UserView> people = people(entitlements.stream().map(CourseEntitlementInfo::getUserId).distinct().toList());

        List<CourseLearnersView.Row> rows = entitlements.stream()
                .collect(Collectors.toMap(CourseEntitlementInfo::getUserId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .map(e -> learnerRow(e, progress.get(e.getUserId()), activity.get(e.getUserId()), certificates.get(e.getUserId()), people.get(e.getUserId())))
                .toList();

        long completed = rows.stream().filter(InstructorCourseService::isCompleted).count();
        long inactive = rows.stream().filter(r -> isInactive(r, inactiveBefore)).count();
        CourseLearnersView.Counts counts = new CourseLearnersView.Counts(rows.size(), rows.size() - completed - inactive, completed, inactive);
        long newIn30 = rows.stream().filter(r -> r.joinedAt() != null && r.joinedAt().isAfter(now.minusDays(30))).count();

        String needle = fold(query.q());
        List<CourseLearnersView.Row> filtered = rows.stream()
                .filter(r -> needle.isEmpty() || fold(r.name()).contains(needle))
                .filter(r -> switch (query.filter() == null ? LearnerFilter.ALL : query.filter()) {
                    case ALL -> true;
                    case COMPLETED -> isCompleted(r);
                    case INACTIVE -> isInactive(r, inactiveBefore);
                    case ACTIVE -> !isCompleted(r) && !isInactive(r, inactiveBefore);
                })
                .sorted(learnerOrder(query.sort(), query.ascending()))
                .toList();
        int from = Math.min((query.page() - 1) * query.size(), filtered.size());
        int to = Math.min(from + query.size(), filtered.size());
        int totalPages = (int) Math.ceil(filtered.size() / (double) query.size());
        return new CourseLearnersView(new PaginatedMetadata(query.page(), query.size(), filtered.size(), totalPages),
                filtered.subList(from, to), counts, newIn30);
    }

    private static CourseLearnersView.Row learnerRow(CourseEntitlementInfo entitlement, CourseProgressInfo progress,
                                                         EngagementLookup.LearnerActivity activity,
                                                         CertificateRequestInfo certificate, UserView person) {
        int total = progress == null || progress.getTotalItems() == null ? 0 : progress.getTotalItems();
        int done = progress == null || progress.getCompletedItems() == null ? 0 : progress.getCompletedItems();
        CourseLearnersView.CertificateState state = certificate == null ? CourseLearnersView.CertificateState.NONE
                : switch (certificate.getStatus()) {
                    case PENDING -> CourseLearnersView.CertificateState.PENDING;
                    case APPROVED -> CourseLearnersView.CertificateState.ISSUED;
                    default -> CourseLearnersView.CertificateState.REJECTED;
                };
        return new CourseLearnersView.Row(
                entitlement.getUserId(),
                person == null ? null : person.name(),
                person == null ? null : person.avatarUrl(),
                entitlement.getGrantedAt(),
                total == 0 ? 0 : (int) Math.round(done * 100.0 / total),
                done,
                total,
                progress == null ? null : progress.getAvgScore(),
                activity == null ? null : activity.getLastActivityAt(),
                activity == null || activity.getActiveSeconds() == null ? 0 : activity.getActiveSeconds(),
                state,
                certificate == null ? null : certificate.getId());
    }

    private static boolean isCompleted(CourseLearnersView.Row row) {
        return row.totalItems() > 0 && row.completedItems() >= row.totalItems();
    }

    private static boolean isInactive(CourseLearnersView.Row row, LocalDateTime before) {
        return !isCompleted(row) && (row.lastActivityAt() == null || row.lastActivityAt().isBefore(before));
    }

    private static Comparator<CourseLearnersView.Row> learnerOrder(LearnerSort sort, boolean ascending) {
        Comparator<CourseLearnersView.Row> order = switch (sort == null ? LearnerSort.JOINED : sort) {
            case JOINED -> Comparator.comparing(CourseLearnersView.Row::joinedAt, Comparator.nullsFirst(Comparator.naturalOrder()));
            case PROGRESS -> Comparator.comparingInt(CourseLearnersView.Row::progressPercent);
            case LAST_ACTIVITY -> Comparator.comparing(CourseLearnersView.Row::lastActivityAt, Comparator.nullsFirst(Comparator.naturalOrder()));
            case SCORE -> Comparator.comparing(CourseLearnersView.Row::avgScore, Comparator.nullsFirst(Comparator.naturalOrder()));
        };
        return ascending ? order : order.reversed();
    }

    // ---- lesson reach --------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public LessonReachView lessonReach(String userId, String courseId) {
        authorizationService.requireCourseInstructor(userId, courseId);
        long learners = progressLookup.courseProgressOfCourse(courseId).size();
        if (learners == 0) {
            return new LessonReachView(0, List.of());
        }
        Map<String, Long> completed = progressLookup.completedPerItem(courseId).stream()
                .collect(Collectors.toMap(ProgressLookup.ItemCount::itemId, ProgressLookup.ItemCount::total));
        Map<String, Long> questions = communityLookup.questionCountPerLesson(courseId);
        List<LessonInfo> lessons = catalogLookup.readableLessons(courseId);
        Map<String, SectionPlace> places = curriculum.sectionPlaces(
                lessons.stream().map(LessonInfo::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<LessonReachView.Lesson> rows = new ArrayList<>();
        for (int i = 0; i < lessons.size(); i++) {
            LessonInfo lesson = lessons.get(i);
            long done = completed.getOrDefault(lesson.getStableId(), 0L);
            rows.add(new LessonReachView.Lesson(lesson.getStableId(), i + 1, places.get(lesson.getSectionId()).title(), lesson.getTitle(),
                    lesson.getLessonType(), done, Math.round(done * 1000.0 / learners) / 10.0,
                    questions.getOrDefault(lesson.getStableId(), 0L)));
        }
        return new LessonReachView(learners, rows);
    }

    // ---- quizzes -------------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<QuizSummary> quizzes(String userId, String courseId) {
        authorizationService.requireCourseInstructor(userId, courseId);
        List<LessonInfo> lessons = catalogLookup.readableLessons(courseId);
        List<LessonInfo> quizzes = lessons.stream().filter(l -> l.getLessonType() == LessonType.QUIZ).toList();
        if (quizzes.isEmpty()) {
            return List.of();
        }
        List<String> quizIds = quizzes.stream().map(LessonInfo::getStableId).toList();
        Map<String, Long> questionCounts = catalogLookup.quizQuestionCounts(quizIds);
        Map<String, List<QuizSessionInfo>> sessions = assessmentLookup.quizSessionsOfCourse(courseId,
                        List.of(QuizSessionStatus.SUBMITTED, QuizSessionStatus.GRADED, QuizSessionStatus.PENDING_GRADE))
                .stream().filter(s -> s.getQuizLessonId() != null && s.getUserId() != null)
                .collect(Collectors.groupingBy(QuizSessionInfo::getQuizLessonId));
        Map<String, Set<String>> passed = progressLookup.passedIn(courseId, quizIds).stream()
                .collect(Collectors.groupingBy(ProgressLookup.PassedRow::itemId,
                        Collectors.mapping(ProgressLookup.PassedRow::userId, Collectors.toSet())));
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < lessons.size(); i++) {
            positions.put(lessons.get(i).getStableId(), i + 1);
        }
        return quizzes.stream().map(quiz -> {
            List<QuizSessionInfo> all = sessions.getOrDefault(quiz.getStableId(), List.of());
            List<QuizSessionInfo> scored = all.stream().filter(s -> s.getStatus() != QuizSessionStatus.PENDING_GRADE).toList();
            Map<String, QuizSessionInfo> latest = scored.stream().collect(Collectors.toMap(QuizSessionInfo::getUserId, Function.identity(),
                    (a, b) -> Comparator.comparing(QuizSessionInfo::getEndTime, Comparator.nullsFirst(Comparator.naturalOrder())).compare(a, b) >= 0 ? a : b));
            Map<String, QuizSessionSubmissionInfo> submissions = assessmentLookup
                    .submissionsOfQuizSessions(latest.values().stream().map(QuizSessionInfo::getId).toList()).stream()
                    .collect(Collectors.toMap(QuizSessionSubmissionInfo::getQuizSessionId, Function.identity(), (a, b) -> a));
            OptionalDouble average = latest.values().stream()
                    .map(s -> submissions.get(s.getId()))
                    .filter(sub -> sub != null && sub.getScore() != null)
                    .mapToDouble(QuizSessionSubmissionInfo::getScore).average();
            Set<String> passers = passed.getOrDefault(quiz.getStableId(), Set.of());
            long attempted = latest.size();
            long passedCount = latest.keySet().stream().filter(passers::contains).count();
            return new QuizSummary(
                    quiz.getStableId(),
                    quiz.getTitle(),
                    positions.getOrDefault(quiz.getStableId(), 0),
                    questionCounts.getOrDefault(quiz.getStableId(), 0L),
                    scored.size(),
                    attempted,
                    average.isPresent() ? Math.round(average.getAsDouble() * 10) / 10.0 : null,
                    attempted == 0 ? null : Math.round(passedCount * 1000.0 / attempted) / 10.0,
                    all.size() - scored.size());
        }).toList();
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    private static void requirePage(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(ErrorCode.INVALID_PAGINATION, "page must be ≥ 1 and size 1–" + MAX_PAGE_SIZE);
        }
    }

    private Map<String, String> names(List<String> ids) {
        return people(ids).values().stream().filter(p -> p.name() != null)
                .collect(Collectors.toMap(UserView::userId, UserView::name, (a, b) -> a));
    }

    /** Names and avatars from user, in batches; a user-service failure leaves them empty instead of failing the page. */
    private Map<String, UserView> people(List<String> ids) {
        Map<String, UserView> result = new HashMap<>();
        for (int from = 0; from < ids.size(); from += NAME_BATCH) {
            List<String> batch = ids.subList(from, Math.min(ids.size(), from + NAME_BATCH));
            try {
                ApiResponse<List<UserView>> response = userService.getInstructorsByIds(batch);
                if (response != null && response.data() != null) {
                    response.data().forEach(user -> result.putIfAbsent(user.userId(), user));
                }
            } catch (UpstreamCallException e) {
                return result;
            }
        }
        return result;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= DESCRIPTION_LENGTH ? text : text.substring(0, DESCRIPTION_LENGTH);
    }

    /** Lower-case, without Vietnamese diacritics, for accent-insensitive matching. */
    static String fold(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String stripped = Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
