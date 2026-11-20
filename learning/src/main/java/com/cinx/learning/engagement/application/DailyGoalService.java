package com.cinx.learning.engagement.application;

import com.cinx.learning.progress.application.ProgressLookup;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.engagement.application.command.SetDailyGoalCommand;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import com.cinx.learning.engagement.application.mapper.DailyGoalMapper;
import com.cinx.learning.engagement.application.query.DailyGoalView;
import com.cinx.learning.engagement.domain.UserDailyGoal;
import com.cinx.learning.engagement.domain.UserDailyGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DailyGoalService implements IDailyGoalService {
    private final ProgressLookup progressLookup;
    private final UserDailyGoalRepository dailyGoalRepository;
    private final UserExperienceService userExperienceService;
    private final DailyGoalMapper dailyGoalMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DailyGoalView> getUserDailyGoals(String userId, LocalDate date) {
        return dailyGoalRepository.findByUserIdAndGoalDate(userId, date).stream().map(dailyGoalMapper::toView).toList();
    }

    @Override
    public DailyGoalView setDailyGoal(String userId, SetDailyGoalCommand request) {
        DailyGoalType goalType = request.goalType();
        LocalDate goalDate = request.goalDate() != null ? request.goalDate() : LocalDate.now();
        String goalKey = buildGoalKey(goalType, request.targetItemId());
        Integer targetValue = resolveTargetValue(goalType, request.targetValue());

        UserDailyGoal goal = dailyGoalRepository.findByUserIdAndGoalDateAndGoalKey(userId, goalDate, goalKey)
                .orElse(UserDailyGoal.builder()
                        .userId(userId)
                        .goalType(goalType)
                        .goalKey(goalKey)
                        .goalDate(goalDate)
                        .currentValue(0)
                        .isCompleted(false)
                        .build());

        goal.setGoalType(goalType);
        goal.setGoalKey(goalKey);
        goal.setTargetItemId(resolveTargetItemId(goalType, request.targetItemId()));
        goal.setTargetValue(targetValue);
        if (goal.getCurrentValue() == null) {
            goal.setCurrentValue(0);
        }

        if (goalType == DailyGoalType.SPECIFIC_LESSON_COMPLETED) {
            LearningItemProgressInfo progress = progressLookup
                    .itemProgress(request.targetItemId(), userId)
                    .orElseThrow(() -> new BadRequestException("Learning item progress not found for target lesson"));
            if (Boolean.TRUE.equals(progress.getIsCompleted())) {
                goal.setCurrentValue(1);
            }
        }

        if (goal.getCurrentValue() >= goal.getTargetValue()) {
            goal.setCurrentValue(goal.getTargetValue());
            goal.setIsCompleted(true);
        } else {
            goal.setIsCompleted(false);
        }

        return dailyGoalMapper.toView(dailyGoalRepository.save(goal));
    }

    @Override
    public void recordProgress(String userId, DailyGoalType goalType, Integer amount) {
        if (amount == null || amount <= 0) {
            return;
        }
        if (goalType == DailyGoalType.SPECIFIC_LESSON_COMPLETED) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Use recordLessonCompleted for specific lesson goals");
        }

        LocalDate today = LocalDate.now();
        dailyGoalRepository.findByUserIdAndGoalDateAndGoalKey(userId, today, buildGoalKey(goalType, null))
                .ifPresent(goal -> incrementGoal(goal, amount));

        if (goalType == DailyGoalType.XP) {
            addXpToUserProfile(userId, amount);
        }
    }

    @Override
    public void recordLessonCompleted(String userId, String itemId) {
        LocalDate today = LocalDate.now();
        dailyGoalRepository
                .findByUserIdAndGoalDateAndGoalTypeAndTargetItemId(
                        userId,
                        today,
                        DailyGoalType.SPECIFIC_LESSON_COMPLETED,
                        itemId)
                .ifPresent(goal -> incrementGoal(goal, 1));
    }

    @Override
    public void deleteDailyGoal(String userId, LocalDate date, DailyGoalType goalType, String targetItemId) {
        LocalDate goalDate = date != null ? date : LocalDate.now();
        dailyGoalRepository.findByUserIdAndGoalDateAndGoalKey(userId, goalDate, buildGoalKey(goalType, targetItemId))
                .ifPresent(dailyGoalRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyGoalView> getUserDailyGoalsInMonth(String userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return dailyGoalRepository.findByUserIdAndGoalDateBetween(userId, startDate, endDate).stream().map(dailyGoalMapper::toView).toList();
    }

    private void incrementGoal(UserDailyGoal goal, int amount) {
        if (Boolean.TRUE.equals(goal.getIsCompleted())) {
            return;
        }
        int currentValue = goal.getCurrentValue() != null ? goal.getCurrentValue() : 0;
        goal.setCurrentValue(currentValue + amount);
        if (goal.getCurrentValue() >= goal.getTargetValue()) {
            goal.setCurrentValue(goal.getTargetValue());
            goal.setIsCompleted(true);
        } else {
            goal.setIsCompleted(false);
        }
        dailyGoalRepository.save(goal);
    }

    private void addXpToUserProfile(String userId, Integer xpAmount) {
        try {
            userExperienceService.award(userId, xpAmount);
        } catch (Exception e) {
            log.error("Failed to award learning XP for userId={}: {}", userId, e.getMessage());
        }
    }

    private String buildGoalKey(DailyGoalType goalType, String targetItemId) {
        if (goalType == null) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "goalType is required");
        }
        if (goalType == DailyGoalType.SPECIFIC_LESSON_COMPLETED) {
            if (targetItemId == null || targetItemId.isBlank()) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "targetItemId is required for specific lesson goals");
            }
            return goalType.name() + ":" + targetItemId;
        }
        return goalType.name();
    }

    private Integer resolveTargetValue(DailyGoalType goalType, Integer requestedTargetValue) {
        if (goalType == DailyGoalType.SPECIFIC_LESSON_COMPLETED) {
            return 1;
        }
        if (requestedTargetValue == null || requestedTargetValue < 1) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "targetValue must be at least 1");
        }
        return requestedTargetValue;
    }

    private String resolveTargetItemId(DailyGoalType goalType, String targetItemId) {
        return goalType == DailyGoalType.SPECIFIC_LESSON_COMPLETED ? targetItemId : null;
    }
}
