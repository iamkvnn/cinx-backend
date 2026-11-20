package com.cinx.learning.studyplan.application;

import com.cinx.learning.shared.domain.LessonType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Places the remaining lessons of a plan onto days. Pure and deterministic: "today" is an input, nothing is stored.
 * Rules (ui-spec-week.md § Data dependencies): lessons fill free, not-busy days in order from today, each day up to
 * the daily budget (today's budget minus what was already done today); a lesson longer than the budget takes a free
 * day of its own. Past days show only what was completed on them.
 */
public final class StudyPlanScheduler {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Hard stop so a malformed plan can never loop forever (≈ 10 years of days). */
    private static final int MAX_DAYS = 3650;

    private StudyPlanScheduler() {
    }

    public record Unit(String courseId, String lessonId, String lessonTitle, String sectionTitle, LessonType type, int minutes) {
    }

    public record DoneUnit(Unit unit, LocalDate day) {
    }

    public record Input(
            LocalDate today,
            int minutesPerDay,
            Set<DayOfWeek> freeDays,
            Set<LocalDate> daysOff,
            List<Unit> remaining,
            List<DoneUnit> done,
            LocalDate weekStart,
            /* days before the plan existed are never counted as missed */
            LocalDate planStart) {
    }

    public record Session(Unit unit, boolean done) {
    }

    public record Day(LocalDate date, boolean free, boolean dayOff, int capacityMinutes, List<Session> sessions) {
    }

    public record Result(
            LocalDate finishDate,
            int remainingMinutes,
            int remainingLessons,
            List<Day> week,
            int missedDaysLastWeek,
            Map<LocalDate, List<Unit>> planned) {
    }

    /** Video: whole minutes of its real length (at least 1). Otherwise a default per type (D28). */
    public static int estimateMinutes(LessonType type, Integer videoSeconds) {
        if (type == LessonType.VIDEO && videoSeconds != null && videoSeconds > 0) {
            return Math.max(1, (videoSeconds + 59) / 60);
        }
        if (type == null) {
            return 10;
        }
        return switch (type) {
            case VIDEO, QUIZ -> 10;
            case ARTICLE -> 8;
            case ASSIGNMENT -> 30;
        };
    }

    public static Result schedule(Input in) {
        if (in.freeDays().isEmpty() || in.minutesPerDay() <= 0) {
            throw new IllegalArgumentException("A plan needs at least one free day and a positive daily budget");
        }
        Map<LocalDate, Integer> doneMinutes = new HashMap<>();
        for (DoneUnit d : in.done()) {
            doneMinutes.merge(d.day(), d.unit().minutes(), Integer::sum);
        }

        Map<LocalDate, List<Unit>> planned = new LinkedHashMap<>();
        LocalDate cursor = in.today();
        int used = doneMinutes.getOrDefault(in.today(), 0);
        int guard = 0;
        int remainingMinutes = 0;
        for (Unit unit : in.remaining()) {
            remainingMinutes += unit.minutes();
            while (true) {
                if (++guard > MAX_DAYS * 4) {
                    throw new IllegalStateException("Study plan scheduling did not converge");
                }
                if (!usable(in, cursor)) {
                    cursor = cursor.plusDays(1);
                    used = 0;
                    continue;
                }
                int left = in.minutesPerDay() - used;
                boolean fits = unit.minutes() <= left;
                boolean dayIsEmpty = used == 0 && planned.getOrDefault(cursor, List.of()).isEmpty();
                if (fits || dayIsEmpty) {
                    planned.computeIfAbsent(cursor, k -> new ArrayList<>()).add(unit);
                    used += unit.minutes();
                    break;
                }
                cursor = cursor.plusDays(1);
                used = 0;
            }
        }

        LocalDate finish = planned.isEmpty()
                ? in.done().stream().map(DoneUnit::day).max(LocalDate::compareTo).orElse(in.today())
                : planned.keySet().stream().max(LocalDate::compareTo).orElseThrow();

        List<Day> week = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = in.weekStart().plusDays(i);
            List<Session> sessions = new ArrayList<>();
            for (DoneUnit d : in.done()) {
                if (d.day().equals(date)) {
                    sessions.add(new Session(d.unit(), true));
                }
            }
            if (!date.isBefore(in.today())) {
                for (Unit unit : planned.getOrDefault(date, List.of())) {
                    sessions.add(new Session(unit, false));
                }
            }
            boolean free = in.freeDays().contains(date.getDayOfWeek());
            week.add(new Day(date, free, in.daysOff().contains(date), in.minutesPerDay(), sessions));
        }

        int missed = 0;
        for (int i = 1; i <= 7; i++) {
            LocalDate date = in.today().minusDays(i);
            if (!date.isBefore(in.planStart()) && usable(in, date) && doneMinutes.getOrDefault(date, 0) == 0) {
                missed++;
            }
        }
        return new Result(finish, remainingMinutes, in.remaining().size(), week, missed, planned);
    }

    private static boolean usable(Input in, LocalDate date) {
        return in.freeDays().contains(date.getDayOfWeek()) && !in.daysOff().contains(date);
    }
}
