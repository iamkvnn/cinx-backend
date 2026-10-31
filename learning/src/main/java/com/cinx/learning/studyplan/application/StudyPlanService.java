package com.cinx.learning.studyplan.application;

import com.cinx.learning.learningpath.application.LearningPathLookup;

import com.cinx.learning.engagement.application.EngagementLookup;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.enrollment.application.EnrollmentAccess;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.CurriculumLookup;
import com.cinx.learning.catalog.application.query.SectionPlace;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.application.query.CourseImageInfo;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.query.VideoLessonInfo;
import com.cinx.learning.shared.domain.LearningPathStatus;
import com.cinx.learning.engagement.application.query.LearningActivityDailyInfo;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import com.cinx.learning.learningpath.application.query.LearningPathItemInfo;
import com.cinx.learning.learningpath.application.query.UserLearningPathInfo;
import com.cinx.learning.studyplan.application.command.UpsertStudyPlanCommand;
import com.cinx.learning.studyplan.application.query.DayOffView;
import com.cinx.learning.studyplan.application.query.LastActivityView;
import com.cinx.learning.studyplan.application.query.StudyPlanPreviewView;
import com.cinx.learning.studyplan.application.query.StudyPlanView;
import com.cinx.learning.studyplan.domain.StudyPlan;
import com.cinx.learning.studyplan.domain.StudyPlanSource;
import com.cinx.learning.studyplan.domain.StudyPlanRepository;
import com.cinx.learning.studyplan.application.StudyPlanScheduler.DoneUnit;
import com.cinx.learning.studyplan.application.StudyPlanScheduler.Unit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPlanService implements IStudyPlanService {
    private static final int MAX_DAY_OFF_AHEAD = 60;
    private static final int MAX_COURSES = 12;
    private final LearningPathLookup learningPathLookup;
    private final EngagementLookup engagementLookup;
    private final CatalogLookup catalogLookup;
    private final EnrollmentAccess enrollmentAccess;
    private final ProgressLookup progressLookup;
    private final StudyPlanRepository planRepository;
    private final CurriculumLookup curriculum;
    private final NextLessonResolver nextLessonResolver;
    private final Clock clock;

    /** Everything a plan's source resolves to: its lessons in learning order, with progress and course facts. */
    private record Context(
            List<Unit> units,
            Set<String> completed,
            List<DoneUnit> done,
            List<String> courseOrder,
            Map<String, CourseInfo> courses,
            String pathTitle) {
    }

    private record Settings(StudyPlanSource source, String pathId, List<String> courseIds, Set<DayOfWeek> freeDays, int minutesPerDay) {
    }

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public StudyPlanView get(String userId, LocalDate weekStart) {
        StudyPlan plan = planOf(userId);
        return respond(plan, weekStart);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyPlanPreviewView preview(String userId, UpsertStudyPlanCommand request) {
        Settings settings = validated(userId, request);
        StudyPlan draft = StudyPlan.builder()
                .userId(userId)
                .startDate(today())
                .build();
        apply(draft, settings);
        planRepository.findByUserId(userId).ifPresent(existing -> draft.getDaysOff().addAll(existing.getDaysOff()));
        StudyPlanScheduler.Result result = schedule(draft, contextOf(draft), null);
        return new StudyPlanPreviewView(result.finishDate(), result.remainingMinutes(), result.remainingLessons());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> userIdsWithPlan() {
        return planRepository.findAllUserIds();
    }

    @Override
    @Transactional(readOnly = true)
    public LastActivityView lastActivity(String userId, String onlyCourseId) {
        LearningActivityDailyInfo activity = (onlyCourseId == null
                ? engagementLookup.lastActivityWithLesson(userId)
                : engagementLookup.lastActivityWithLesson(userId, onlyCourseId))
                .orElseThrow(() -> new NotFoundException("No learning activity yet"));
        String courseId = activity.getCourseId();
        List<LessonInfo> lessons = catalogLookup.readableLessons(courseId);
        if (lessons.isEmpty()) {
            throw new NotFoundException("No readable lesson in course: " + courseId);
        }
        Set<String> completed = completedIds(userId, lessons.stream().map(LessonInfo::getStableId).toList());
        NextLessonResolver.Next next = nextLessonResolver.resolve(userId, lessons, completed, activity.getLastItemId());
        LessonInfo target = next.lesson();
        String courseTitle = catalogLookup.course(courseId).map(CourseInfo::getTitle).orElse(null);
        SectionPlace place = target.getSectionId() == null ? null
                : curriculum.sectionPlaces(List.of(target.getSectionId())).get(target.getSectionId());
        return new LastActivityView(courseId, courseTitle, target.getStableId(), target.getTitle(),
                place == null ? null : place.title(), target.getLessonType(),
                next.positionSeconds(), next.remainingMinutes(), activity.getLastActivityAt());
    }

    // ----------------------------------------------------------------- writes

    @Override
    @Transactional
    public StudyPlanView upsert(String userId, UpsertStudyPlanCommand request) {
        Settings settings = validated(userId, request);
        StudyPlan plan = planRepository.findByUserId(userId)
                .orElseGet(() -> StudyPlan.builder().userId(userId).startDate(today()).build());
        apply(plan, settings);
        // re-planning from today resets the baseline: "late" is measured against what the learner last agreed to
        plan.getDaysOff().removeIf(day -> day.isBefore(today()));
        StudyPlanScheduler.Result result = schedule(plan, contextOf(plan), null);
        plan.setBaselineFinishDate(result.finishDate());
        StudyPlan saved = planRepository.save(plan);
        return respond(saved, null);
    }

    @Override
    @Transactional
    public void attachPath(String userId, String pathId) {
        // a plan that follows the learner's path follows the new one; a plan over chosen courses is left alone
        planRepository.findByUserId(userId)
                .filter(plan -> plan.getSource() == StudyPlanSource.PATH && !pathId.equals(plan.getPathId()))
                .ifPresent(plan -> {
                    plan.setPathId(pathId);
                    plan.setBaselineFinishDate(schedule(plan, contextOf(plan), null).finishDate());
                    planRepository.save(plan);
                });
    }

    @Override
    @Transactional
    public DayOffView addDayOff(String userId, LocalDate date) {
        StudyPlan plan = planOf(userId);
        LocalDate today = today();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAY_OFF_AHEAD))) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED,
                    "A busy day must be between today and " + MAX_DAY_OFF_AHEAD + " days ahead");
        }
        Context context = contextOf(plan);
        int moved = schedule(plan, context, null).planned().getOrDefault(date, List.of()).size();
        plan.getDaysOff().add(date);
        planRepository.save(plan);
        return new DayOffView(moved, toResponse(plan, context, null));
    }

    @Override
    @Transactional
    public StudyPlanView removeDayOff(String userId, LocalDate date) {
        StudyPlan plan = planOf(userId);
        plan.getDaysOff().remove(date);
        planRepository.save(plan);
        return respond(plan, null);
    }

    // ---------------------------------------------------------------- helpers

    private StudyPlan planOf(String userId) {
        return planRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Study plan not found"));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(StudyPlanScheduler.ZONE));
    }

    private Settings validated(String userId, UpsertStudyPlanCommand request) {
        Set<DayOfWeek> freeDays = EnumSet.noneOf(DayOfWeek.class);
        request.freeDays().forEach(day -> freeDays.add(DayOfWeek.of(day)));
        if (request.source() == StudyPlanSource.PATH) {
            UserLearningPathInfo path = learningPathLookup.currentPath(userId)
                    .orElseThrow(() -> new BadRequestException(ErrorCode.VALIDATION_FAILED, "You have no active learning path"));
            return new Settings(StudyPlanSource.PATH, path.getId(), List.of(), freeDays, request.minutesPerDay());
        }
        List<String> courseIds = request.courseIds() == null ? List.of()
                : request.courseIds().stream().map(String::trim).distinct().toList();
        if (courseIds.isEmpty() || courseIds.size() > MAX_COURSES) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "Pick 1 to " + MAX_COURSES + " courses");
        }
        Set<String> owned = enrollmentAccess.entitledCourseIds(userId, courseIds);
        List<String> notOwned = courseIds.stream().filter(id -> !owned.contains(id)).toList();
        if (!notOwned.isEmpty()) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "You can only plan courses you own: " + notOwned);
        }
        return new Settings(StudyPlanSource.COURSES, null, courseIds, freeDays, request.minutesPerDay());
    }

    private static void apply(StudyPlan plan, Settings settings) {
        plan.setSource(settings.source());
        plan.setPathId(settings.pathId());
        plan.getCourseIds().clear();
        plan.getCourseIds().addAll(settings.courseIds());
        plan.setFreeDays(StudyPlan.encodeFreeDays(settings.freeDays()));
        plan.setMinutesPerDay(settings.minutesPerDay());
    }

    private StudyPlanView respond(StudyPlan plan, LocalDate weekStart) {
        return toResponse(plan, contextOf(plan), weekStart);
    }

    private StudyPlanScheduler.Result schedule(StudyPlan plan, Context context, LocalDate weekStart) {
        LocalDate today = today();
        LocalDate monday = (weekStart == null ? today : weekStart).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Unit> remaining = context.units().stream().filter(unit -> !context.completed().contains(unit.lessonId())).toList();
        return StudyPlanScheduler.schedule(new StudyPlanScheduler.Input(
                today, plan.getMinutesPerDay(), plan.freeWeekdays(), plan.getDaysOff(),
                remaining, context.done(), monday, plan.getStartDate()));
    }

    /** Resolves the plan's source to ordered lessons, with minutes, completion and course facts, in a few queries. */
    private Context contextOf(StudyPlan plan) {
        List<String[]> ordered = new ArrayList<>(); // [courseId, lessonId]
        String pathTitle = null;
        if (plan.getSource() == StudyPlanSource.PATH && plan.getPathId() != null) {
            // a dropped or completed path no longer drives the week; a pending one schedules only the courses owned
            UserLearningPathInfo path = learningPathLookup.path(plan.getPathId())
                    .filter(p -> plan.getUserId().equals(p.getUserId()))
                    .filter(p -> p.getStatus() == LearningPathStatus.ACTIVE || p.getStatus() == LearningPathStatus.PENDING_PAYMENT)
                    .orElse(null);
            if (path != null) {
                pathTitle = path.getTitle();
                Set<String> owned = path.getStatus() == LearningPathStatus.ACTIVE ? null
                        : enrollmentAccess.entitledCourseIds(plan.getUserId(),
                                path.getItems().stream().map(LearningPathItemInfo::getCourseId).distinct().toList());
                path.getItems().stream()
                        .sorted(Comparator.comparing(LearningPathItemInfo::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                        .filter(item -> item.getCourseId() != null && item.getLessonId() != null)
                        .filter(item -> owned == null || owned.contains(item.getCourseId()))
                        .forEach(item -> ordered.add(new String[]{item.getCourseId(), item.getLessonId()}));
            }
        }
        Set<String> courseSet = new LinkedHashSet<>();
        if (plan.getSource() == StudyPlanSource.COURSES) {
            courseSet.addAll(plan.getCourseIds());
        } else {
            ordered.forEach(pair -> courseSet.add(pair[0]));
        }
        List<String> courseOrder = new ArrayList<>(courseSet);
        if (courseOrder.isEmpty()) {
            return new Context(List.of(), Set.of(), List.of(), List.of(), Map.of(), pathTitle);
        }

        List<LessonInfo> lessons = catalogLookup.readableLessonsOfCourses(courseOrder);
        Map<String, SectionPlace> places = curriculum.sectionPlaces(
                lessons.stream().map(LessonInfo::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<String, LessonInfo> byStableId = lessons.stream()
                .collect(Collectors.toMap(LessonInfo::getStableId, Function.identity(), (a, b) -> a));
        if (plan.getSource() == StudyPlanSource.COURSES) {
            Map<String, List<LessonInfo>> byCourse = new LinkedHashMap<>();
            lessons.forEach(lesson -> byCourse.computeIfAbsent(places.get(lesson.getSectionId()).courseId(), k -> new ArrayList<>()).add(lesson));
            for (String courseId : courseOrder) {
                byCourse.getOrDefault(courseId, List.of()).forEach(lesson -> ordered.add(new String[]{courseId, lesson.getStableId()}));
            }
        }

        List<String> videoIds = lessons.stream()
                .filter(lesson -> lesson.getLessonType() == LessonType.VIDEO)
                .map(LessonInfo::getStableId)
                .toList();
        Map<String, Integer> videoSeconds = videoIds.isEmpty() ? Map.of()
                : catalogLookup.videoLessons(videoIds).stream()
                        .filter(video -> video.getDuration() != null)
                        .collect(Collectors.toMap(VideoLessonInfo::getLessonId, VideoLessonInfo::getDuration, (a, b) -> a));

        List<Unit> units = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String[] pair : ordered) {
            LessonInfo lesson = byStableId.get(pair[1]);
            if (lesson == null || !seen.add(pair[1])) {
                continue;
            }
            units.add(new Unit(pair[0], lesson.getStableId(), lesson.getTitle(),
                    places.get(lesson.getSectionId()) == null ? null : places.get(lesson.getSectionId()).title(), lesson.getLessonType(),
                    StudyPlanScheduler.estimateMinutes(lesson.getLessonType(), videoSeconds.get(lesson.getStableId()))));
        }

        List<LearningItemProgressInfo> progress = units.isEmpty() ? List.of()
                : progressLookup.itemProgressOfUser(plan.getUserId(), units.stream().map(Unit::lessonId).toList());
        Map<String, Unit> unitById = units.stream().collect(Collectors.toMap(Unit::lessonId, Function.identity()));
        Set<String> completed = new HashSet<>();
        List<DoneUnit> done = new ArrayList<>();
        for (LearningItemProgressInfo item : progress) {
            if (!Boolean.TRUE.equals(item.getIsCompleted())) {
                continue;
            }
            completed.add(item.getItemId());
            Unit unit = unitById.get(item.getItemId());
            if (unit != null && item.getCompletedAt() != null) {
                LocalDate day = item.getCompletedAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(StudyPlanScheduler.ZONE).toLocalDate();
                done.add(new DoneUnit(unit, day));
            }
        }

        Map<String, CourseInfo> courses = catalogLookup.courses(courseOrder).stream()
                .collect(Collectors.toMap(CourseInfo::getId, Function.identity()));
        return new Context(units, completed, done, courseOrder, courses, pathTitle);
    }

    private StudyPlanView toResponse(StudyPlan plan, Context context, LocalDate weekStart) {
        StudyPlanScheduler.Result result = schedule(plan, context, weekStart);
        Map<String, String> courseTitles = context.courses().values().stream()
                .collect(Collectors.toMap(CourseInfo::getId, course -> Objects.requireNonNullElse(course.getTitle(), "")));

        List<StudyPlanView.Course> courses = context.courseOrder().stream().map(courseId -> {
            List<Unit> ofCourse = context.units().stream().filter(unit -> unit.courseId().equals(courseId)).toList();
            int completedCount = (int) ofCourse.stream().filter(unit -> context.completed().contains(unit.lessonId())).count();
            CourseInfo course = context.courses().get(courseId);
            return new StudyPlanView.Course(courseId, course == null ? null : course.getTitle(),
                    course == null ? null : thumbnail(course), completedCount, ofCourse.size());
        }).toList();

        List<StudyPlanView.Day> days = result.week().stream().map(day -> new StudyPlanView.Day(
                day.date(), day.free(), day.dayOff(), day.capacityMinutes(),
                day.sessions().stream().map(session -> {
                    Unit unit = session.unit();
                    return new StudyPlanView.Session(unit.courseId(), courseTitles.get(unit.courseId()), unit.lessonId(),
                            unit.lessonTitle(), unit.sectionTitle(), unit.type(), unit.minutes(), session.done() ? "DONE" : "PLANNED");
                }).toList())).toList();

        long lateDays = plan.getBaselineFinishDate() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(plan.getBaselineFinishDate(), result.finishDate()));
        List<Integer> freeDays = plan.freeWeekdays().stream().map(DayOfWeek::getValue).sorted().toList();
        LocalDate from = result.week().get(0).date();
        return new StudyPlanView(plan.getSource(), plan.getMinutesPerDay(), freeDays, today(), plan.getStartDate(),
                result.finishDate(), plan.getBaselineFinishDate(), lateDays, result.missedDaysLastWeek(),
                result.remainingLessons(), result.remainingMinutes(), context.pathTitle(), courses,
                new StudyPlanView.Week(from, from.plusDays(6), days));
    }

    private Set<String> completedIds(String userId, List<String> lessonIds) {
        return progressLookup.itemProgressOfUser(userId, lessonIds).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                .map(LearningItemProgressInfo::getItemId)
                .collect(Collectors.toSet());
    }

    private static String thumbnail(CourseInfo course) {
        return course.getImages() == null ? null : course.getImages().stream()
                .map(CourseImageInfo::getImageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
