package com.cinx.learning.reminder.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.engagement.application.EngagementLookup;
import com.cinx.learning.progress.application.ProgressLookup;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.reminder.domain.ReminderAuthor;
import com.cinx.learning.reminder.domain.StudyReminderLogRepository;
import com.cinx.learning.reminder.domain.StudyReminderPreference;
import com.cinx.learning.reminder.domain.StudyReminderPreferenceRepository;
import com.cinx.learning.review.application.IReviewService;
import com.cinx.learning.studyplan.application.IStudyPlanService;
import com.cinx.learning.studyplan.application.StudyPlanScheduler;
import com.cinx.learning.studyplan.application.query.LastActivityView;
import com.cinx.learning.studyplan.application.query.StudyPlanView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chooses this hour's study reminders (plan P4-07 §6.1): who, why and where it opens. The assistant may only reword a
 * reminder; it never picks the learner, the hour or the reason.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyReminderService {
    static final int STREAK_MIN_DAYS = 2;
    static final int INACTIVE_FROM_DAYS = 3;
    static final int INACTIVE_TO_DAYS = 30;
    static final int PAGE = 200;
    /** At most this many reminders in any 7 days, so a busy week is not a daily nag. */
    static final int MAX_PER_WEEK = 4;
    /** Past this much time spent waiting on the assistant in one run, the rest of the run sends templates. */
    static final Duration AI_BUDGET = Duration.ofMinutes(10);

    private final Clock clock;
    private final IStudyPlanService studyPlans;
    private final EngagementLookup engagement;
    private final ProgressLookup progress;
    private final StudyReminderPreferenceRepository preferences;
    private final StudyReminderLogRepository logs;
    private final ReminderTextWriter writer;
    private final StudyReminderSender sender;
    private final IReviewService reviews;

    /** Sends the reminders due at the current hour (Vietnam time) and returns how many went out. */
    public int runHour() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(StudyPlanScheduler.ZONE));
        int hour = now.getHour();
        if (hour < StudyReminderPreference.FIRST_HOUR || hour > StudyReminderPreference.LAST_HOUR) {
            return 0;
        }
        LocalDate today = now.toLocalDate();
        Set<String> candidates = new LinkedHashSet<>(studyPlans.userIdsWithPlan());
        candidates.addAll(engagement.userIdsWithStreakAtRisk(today, STREAK_MIN_DAYS));
        candidates.addAll(engagement.userIdsWithPendingGoals(today));
        candidates.addAll(engagement.userIdsLastActiveBetween(today.minusDays(INACTIVE_TO_DAYS), today.minusDays(INACTIVE_FROM_DAYS)));
        candidates.addAll(reviews.userIdsWithDue(today));

        Duration aiSpent = Duration.ZERO;
        int sent = 0;
        List<String> all = new ArrayList<>(candidates);
        for (int from = 0; from < all.size(); from += PAGE) {
            List<String> page = all.subList(from, Math.min(all.size(), from + PAGE));
            Map<String, StudyReminderPreference> stored = preferences.findByUserIds(page).stream()
                    .collect(Collectors.toMap(StudyReminderPreference::getUserId, Function.identity()));
            for (String userId : page) {
                StudyReminderPreference preference = stored.getOrDefault(userId, StudyReminderPreference.defaultFor(userId));
                if (!preference.isEnabled() || preference.getHour() != hour) {
                    continue;
                }
                try {
                    if (logs.existsByUserIdAndReminderDate(userId, today) || engagement.activeOn(userId, today)
                            || logs.countByUserIdAndReminderDateBetween(userId, today.minusDays(6), today.minusDays(1)) >= MAX_PER_WEEK) {
                        continue;
                    }
                    Optional<ReminderMessage> message = choose(userId, today);
                    if (message.isEmpty()) {
                        continue;
                    }
                    Instant asked = clock.instant();
                    boolean useAi = aiSpent.compareTo(AI_BUDGET) < 0;
                    Optional<ReminderTextWriter.Text> rewritten = useAi ? writer.rewrite(message.get()) : Optional.empty();
                    if (useAi) {
                        aiSpent = aiSpent.plus(Duration.between(asked, clock.instant()));
                    }
                    ReminderMessage text = rewritten.map(r -> message.get().withText(r.title(), r.body())).orElse(message.get());
                    sender.send(userId, today, text, rewritten.isPresent() ? ReminderAuthor.AI : ReminderAuthor.TEMPLATE);
                    sent++;
                } catch (DataIntegrityViolationException duplicate) {
                    // another replica or run sent today's reminder first
                    log.debug("Study reminder for {} on {} was already sent", userId, today);
                } catch (RuntimeException e) {
                    log.warn("Study reminder for {} failed: {}", userId, e.getMessage());
                }
            }
        }
        log.info("Study reminders at {}:00 {}: {} candidates, {} sent", hour, today, all.size(), sent);
        return sent;
    }

    /** The first reason that applies, in {@link com.cinx.learning.reminder.domain.ReminderReason} order. */
    Optional<ReminderMessage> choose(String userId, LocalDate today) {
        return planSession(userId)
                .or(() -> streakAtRisk(userId, today))
                .or(() -> reviewsDue(userId, today))
                .or(() -> dailyGoal(userId, today))
                .or(() -> inactive(userId, today));
    }

    private Optional<ReminderMessage> reviewsDue(String userId, LocalDate today) {
        long due = reviews.dueCount(userId, today);
        // the queue shows at most a day's cap, so the reminder never promises more
        return due > 0 ? Optional.of(ReminderTemplates.reviewsDue((int) Math.min(due, IReviewService.DAILY_CAP))) : Optional.empty();
    }

    private Optional<ReminderMessage> planSession(String userId) {
        StudyPlanView plan;
        try {
            plan = studyPlans.get(userId, null);
        } catch (NotFoundException noPlan) {
            return Optional.empty();
        }
        List<StudyPlanView.Session> pending = plan.week().days().stream()
                .filter(day -> day.date().equals(plan.today()))
                .flatMap(day -> day.sessions().stream())
                .filter(session -> !"DONE".equals(session.status()))
                .toList();
        if (pending.isEmpty()) {
            return Optional.empty();
        }
        StudyPlanView.Session first = pending.get(0);
        int minutes = pending.stream().mapToInt(StudyPlanView.Session::minutes).sum();
        return Optional.of(ReminderTemplates.planSession(first.courseId(), first.courseTitle(), first.lessonId(),
                first.lessonTitle(), minutes));
    }

    private Optional<ReminderMessage> streakAtRisk(String userId, LocalDate today) {
        int streak = engagement.currentStreak(userId);
        boolean extendedYesterday = engagement.lastActiveDay(userId).filter(today.minusDays(1)::equals).isPresent();
        if (streak < STREAK_MIN_DAYS || !extendedYesterday) {
            return Optional.empty();
        }
        Optional<LastActivityView> resume = resume(userId);
        return Optional.of(ReminderTemplates.streakAtRisk(streak, resume.map(LastActivityView::courseId).orElse(null),
                resume.map(LastActivityView::lessonId).orElse(null), resume.map(LastActivityView::lessonTitle).orElse(null)));
    }

    private Optional<ReminderMessage> dailyGoal(String userId, LocalDate today) {
        return engagement.pendingGoals(userId, today).stream().findFirst()
                .map(goal -> ReminderTemplates.dailyGoal(goal.getGoalType(), Math.max(1, goal.getTargetValue() - goal.getCurrentValue())));
    }

    private Optional<ReminderMessage> inactive(String userId, LocalDate today) {
        Optional<LocalDate> last = engagement.lastActiveDay(userId);
        if (last.isEmpty()) {
            return Optional.empty();
        }
        long days = ChronoUnit.DAYS.between(last.get(), today);
        if (days < INACTIVE_FROM_DAYS || days > INACTIVE_TO_DAYS) {
            return Optional.empty();
        }
        return resume(userId)
                .filter(lesson -> !completed(userId, lesson.courseId()))
                .map(lesson -> ReminderTemplates.inactive((int) days, lesson.courseId(), lesson.courseTitle(),
                        lesson.lessonId(), lesson.lessonTitle()));
    }

    private Optional<LastActivityView> resume(String userId) {
        try {
            return Optional.ofNullable(studyPlans.lastActivity(userId, null));
        } catch (NotFoundException nothingYet) {
            return Optional.empty();
        }
    }

    private boolean completed(String userId, String courseId) {
        return progress.courseProgressOfUser(userId, List.of(courseId)).stream()
                .map(CourseProgressInfo::getIsCompleted)
                .anyMatch(Boolean.TRUE::equals);
    }
}
