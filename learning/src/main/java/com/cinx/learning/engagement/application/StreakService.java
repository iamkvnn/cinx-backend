package com.cinx.learning.engagement.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.engagement.application.query.UserStreakView;
import com.cinx.learning.engagement.application.mapper.UserStreakMapper;
import com.cinx.learning.engagement.domain.UserStreak;
import com.cinx.learning.engagement.domain.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StreakService implements IStreakService {
    /** A study day is a Vietnam day, as for reviews and the study plan; the JVM runs in UTC. */
    static final ZoneId STREAK_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** The milestones the web app celebrates over the whole window. */
    static final List<Integer> MILESTONES = List.of(7, 30, 100);
    /** A shorter streak had barely lit; losing it is not announced. */
    static final int LOST_MIN = 3;

    private final UserStreakRepository streakRepository;
    private final UserStreakMapper streakMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public UserStreakView getUserStreak(String userId) {
        UserStreak streak = streakRepository.findByUserId(userId)
                .orElseGet(() -> UserStreak.builder()
                        .userId(userId)
                        .currentStreak(0)
                        .highestStreak(0)
                        .build());
        return toView(streak, LocalDate.now(clock.withZone(STREAK_ZONE)));
    }

    @Override
    public UserStreakView markSeen(String userId, Integer milestone, boolean lost) {
        if ((milestone != null) == lost) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Give either a milestone or lost");
        }
        LocalDate today = LocalDate.now(clock.withZone(STREAK_ZONE));
        UserStreak streak = streakRepository.findForUpdateByUserId(userId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.BAD_REQUEST, "There is no streak yet"));
        if (milestone != null) {
            if (!MILESTONES.contains(milestone) || milestone > toView(streak, today).getCurrentStreak()) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "milestone must be 7, 30 or 100 and already reached");
            }
            streak.setCelebratedMilestone(Math.max(celebrated(streak), milestone));
        } else {
            streak.setLostNoticedOn(streak.getLastActivityDate());
        }
        streakRepository.save(streak);
        return toView(streak, today);
    }

    private UserStreakView toView(UserStreak streak, LocalDate today) {
        UserStreakView view = streakMapper.toDto(streak);
        view.setCelebratedMilestone(celebrated(streak));
        // a streak whose last day is before yesterday is already broken; the stored count only resets on the next activity
        LocalDate lastActivity = streak.getLastActivityDate();
        if (lastActivity == null || lastActivity.isBefore(today.minusDays(1))) {
            view.setCurrentStreak(0);
            int broken = streak.getCurrentStreak() == null ? 0 : streak.getCurrentStreak();
            if (lastActivity != null && broken >= LOST_MIN && !lastActivity.equals(streak.getLostNoticedOn())) {
                view.setLostStreak(broken);
            }
        }
        return view;
    }

    private static int celebrated(UserStreak streak) {
        return streak.getCelebratedMilestone() == null ? 0 : streak.getCelebratedMilestone();
    }

    @Override
    public void updateStreakOnActivity(String userId) {
        LocalDate today = LocalDate.now(clock.withZone(STREAK_ZONE));
        Optional<UserStreak> streakOpt = streakRepository.findForUpdateByUserId(userId);

        UserStreak streak;
        if (streakOpt.isPresent()) {
            streak = streakOpt.get();
            LocalDate lastActivity = streak.getLastActivityDate();

            if (lastActivity == null || lastActivity.isBefore(today.minusDays(1))) {
                // Streak broken or just started; its milestones can be celebrated again
                streak.setCurrentStreak(1);
                streak.setCelebratedMilestone(0);
            } else if (lastActivity.equals(today.minusDays(1))) {
                // Continued streak
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            }
            // IF lastActivity.equals(today) do nothing to the count
            
            if (streak.getCurrentStreak() > streak.getHighestStreak()) {
                streak.setHighestStreak(streak.getCurrentStreak());
            }
            streak.setLastActivityDate(today);
        } else {
            streak = UserStreak.builder()
                    .userId(userId)
                    .currentStreak(1)
                    .highestStreak(1)
                    .lastActivityDate(today)
                    .build();
        }
        streakRepository.save(streak);
    }
}
