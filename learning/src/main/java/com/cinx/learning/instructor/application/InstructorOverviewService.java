package com.cinx.learning.instructor.application;

import com.cinx.learning.community.application.CommunityLookup;

import com.cinx.learning.enrollment.application.EnrollmentAccess;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.instructor.application.query.InboxView;
import com.cinx.learning.instructor.application.query.OverviewView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * New learners come from course entitlements (granted access, whatever the source), bucketed by day in the business
 * zone; ratings from reviews; waiting work from the inbox. Money is not here: commerce serves revenue.
 */
@Service
@RequiredArgsConstructor
public class InstructorOverviewService implements IInstructorOverviewService {
    private final CommunityLookup communityLookup;
    private final EnrollmentAccess enrollmentAccess;
    private final ProgressLookup progressLookup;
    static final long MAX_DAYS = 400;

    private final InstructorCourseLookup courseLookup;
    private final IInstructorInboxService inboxService;

    /** Not one transaction: it reads the inbox, whose sources may fail on their own without failing this. */
    @Override
    public OverviewView overview(String instructorId, LocalDate from, LocalDate to, String courseId) {
        long days = validateRange(from, to);
        List<InstructorCourseLookup.CourseCard> courses = courseLookup.managedCourses(instructorId).stream()
                .filter(c -> courseId == null || courseId.isBlank() || "all".equals(courseId) || c.id().equals(courseId))
                .toList();
        List<String> ids = courses.stream().map(InstructorCourseLookup.CourseCard::id).toList();

        LocalDate previousFrom = from.minusDays(days);
        LocalDateTime start = wallTime(from);
        LocalDateTime end = wallTime(to.plusDays(1));
        LocalDateTime previousStart = wallTime(previousFrom);

        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDay.put(day, 0L);
        }
        Map<String, Long> newPerCourse = new LinkedHashMap<>();
        long current = 0;
        long previous = 0;
        if (!ids.isEmpty()) {
            for (EnrollmentAccess.GrantRow row : enrollmentAccess.grantsBetween(ids, previousStart, end)) {
                if (row.grantedAt().isBefore(start)) {
                    previous++;
                    continue;
                }
                current++;
                newPerCourse.merge(row.courseId(), 1L, Long::sum);
                LocalDate day = row.grantedAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(InstructorInboxService.BUSINESS_ZONE).toLocalDate();
                byDay.computeIfPresent(day, (d, n) -> n + 1);
            }
        }

        Map<String, Long> learners = ids.isEmpty() ? Map.of() : enrollmentAccess.countPerCourse(ids).stream()
                .collect(Collectors.toMap(EnrollmentAccess.CourseCount::courseId, EnrollmentAccess.CourseCount::total));
        Map<String, Integer> completion = ids.isEmpty() ? Map.of() : progressLookup.completionPerCourse(ids).stream()
                .filter(row -> row.total() > 0)
                .collect(Collectors.toMap(ProgressLookup.CompletionRow::courseId,
                        row -> (int) Math.round((row.completed() == null ? 0 : row.completed()) * 100.0 / row.total())));
        long unreplied = ids.isEmpty() ? 0 : communityLookup.unrepliedReviewGroups(ids).stream()
                .mapToLong(CommunityLookup.UnrepliedGroup::getWaiting).sum();

        InboxView inbox = inboxService.inbox(instructorId, null, null, null);
        Map<String, Long> waitingPerCourse = inbox.groups().stream()
                .collect(Collectors.groupingBy(InboxView.Group::courseId, Collectors.summingLong(InboxView.Group::count)));
        long waiting = inbox.groups().stream().filter(g -> ids.contains(g.courseId())).mapToLong(InboxView.Group::count).sum();
        long overdue = inbox.groups().stream()
                .filter(g -> ids.contains(g.courseId()) && g.urgency() == InboxView.Urgency.OVERDUE)
                .mapToLong(InboxView.Group::count).sum();

        List<OverviewView.CourseRow> rows = new ArrayList<>();
        for (InstructorCourseLookup.CourseCard course : courses) {
            rows.add(new OverviewView.CourseRow(
                    course.id(),
                    course.title(),
                    course.thumbnailUrl(),
                    course.status(),
                    course.publishStatus(),
                    learners.getOrDefault(course.id(), 0L),
                    newPerCourse.getOrDefault(course.id(), 0L),
                    completion.get(course.id()),
                    course.rating(),
                    waitingPerCourse.getOrDefault(course.id(), 0L)));
        }

        return new OverviewView(
                current,
                previous,
                learners.values().stream().mapToLong(Long::longValue).sum(),
                ids.isEmpty() ? null : communityLookup.averageRating(ids),
                ids.isEmpty() ? null : communityLookup.averageRatingBefore(ids, start),
                ids.isEmpty() ? 0 : communityLookup.reviewCount(ids),
                unreplied,
                waiting,
                overdue,
                byDay.entrySet().stream().map(e -> new OverviewView.DayCount(e.getKey(), e.getValue())).toList(),
                rows);
    }

    @Override
    public String overviewCsv(String instructorId, LocalDate from, LocalDate to) {
        OverviewView overview = overview(instructorId, from, to, null);
        StringBuilder csv = new StringBuilder("courseId,title,learners,newLearners,completionPercent,rating,waiting\n");
        for (OverviewView.CourseRow row : overview.courses()) {
            csv.append(row.courseId()).append(',')
                    .append(quote(row.title())).append(',')
                    .append(row.learners()).append(',')
                    .append(row.newLearners()).append(',')
                    .append(row.completionPercent() == null ? "" : row.completionPercent()).append(',')
                    .append(row.rating() == null ? "" : row.rating()).append(',')
                    .append(row.waiting()).append('\n');
        }
        return csv.toString();
    }

    private static long validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID, "from and to are required and from must not be after to");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_DAYS) {
            throw new BadRequestException(ErrorCode.STATISTICS_RANGE_TOO_LARGE, "The range can be at most " + MAX_DAYS + " days");
        }
        return days;
    }

    /** Midnight of a business day, as the JVM wall time the entitlement column holds. */
    private static LocalDateTime wallTime(LocalDate day) {
        return day.atStartOfDay(InstructorInboxService.BUSINESS_ZONE).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    /** CSV-escapes a title and neutralises a leading formula character. */
    private static String quote(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
