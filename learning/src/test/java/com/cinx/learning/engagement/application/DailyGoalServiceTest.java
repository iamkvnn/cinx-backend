package com.cinx.learning.engagement.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.learning.engagement.application.command.SetDailyGoalCommand;
import com.cinx.learning.engagement.application.mapper.DailyGoalMapper;
import com.cinx.learning.engagement.application.mapper.DailyGoalMapperImpl;
import com.cinx.learning.engagement.application.query.DailyGoalView;
import com.cinx.learning.engagement.domain.UserDailyGoal;
import com.cinx.learning.engagement.domain.UserDailyGoalRepository;
import com.cinx.learning.progress.application.ProgressLookup;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import com.cinx.learning.shared.domain.DailyGoalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyGoalServiceTest {

    @Mock
    private UserDailyGoalRepository dailyGoalRepository;

    @Mock
    private ProgressLookup progressLookup;

    @Mock
    private UserExperienceService userExperienceService;

    @Spy
    private DailyGoalMapper dailyGoalMapper = new DailyGoalMapperImpl();

    @InjectMocks
    private DailyGoalService dailyGoalService;

    @Test
    void setDailyGoalCreatesGenericGoalByType() {
        LocalDate goalDate = LocalDate.of(2026, 5, 18);
        when(dailyGoalRepository.findByUserIdAndGoalDateAndGoalKey("user-1", goalDate, "XP"))
                .thenReturn(Optional.empty());
        when(dailyGoalRepository.save(any(UserDailyGoal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyGoalView goal = dailyGoalService.setDailyGoal(
                "user-1",
                new SetDailyGoalCommand(DailyGoalType.XP, 100, goalDate, null));

        assertThat(goal.goalType()).isEqualTo(DailyGoalType.XP);
        assertThat(goal.targetValue()).isEqualTo(100);
        assertThat(goal.currentValue()).isZero();
        assertThat(goal.isCompleted()).isFalse();
        assertThat(goal.goalDate()).isEqualTo(goalDate);
    }

    @Test
    void setDailyGoalCompletesSpecificLessonGoalWhenLessonAlreadyCompleted() {
        LocalDate goalDate = LocalDate.of(2026, 5, 18);
        LearningItemProgressInfo progress = LearningItemProgressInfo.builder().itemId("lesson-1").isCompleted(true).build();

        when(dailyGoalRepository.findByUserIdAndGoalDateAndGoalKey(
                "user-1",
                goalDate,
                "SPECIFIC_LESSON_COMPLETED:lesson-1"))
                .thenReturn(Optional.empty());
        when(progressLookup.itemProgress("lesson-1", "user-1"))
                .thenReturn(Optional.of(progress));
        when(dailyGoalRepository.save(any(UserDailyGoal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyGoalView goal = dailyGoalService.setDailyGoal(
                "user-1",
                new SetDailyGoalCommand(DailyGoalType.SPECIFIC_LESSON_COMPLETED, null, goalDate, "lesson-1"));

        assertThat(goal.targetItemId()).isEqualTo("lesson-1");
        assertThat(goal.targetValue()).isEqualTo(1);
        assertThat(goal.currentValue()).isEqualTo(1);
        assertThat(goal.isCompleted()).isTrue();
    }

    @Test
    void setDailyGoalRejectsSpecificLessonGoalWithoutProgress() {
        LocalDate goalDate = LocalDate.of(2026, 5, 18);
        when(dailyGoalRepository.findByUserIdAndGoalDateAndGoalKey(
                "user-1",
                goalDate,
                "SPECIFIC_LESSON_COMPLETED:lesson-1"))
                .thenReturn(Optional.empty());
        when(progressLookup.itemProgress("lesson-1", "user-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dailyGoalService.setDailyGoal(
                "user-1",
                new SetDailyGoalCommand(DailyGoalType.SPECIFIC_LESSON_COMPLETED, null, goalDate, "lesson-1")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recordProgressIncrementsGenericGoalAndAwardsLearningXp() {
        UserDailyGoal goal = UserDailyGoal.builder()
                .userId("user-1")
                .goalType(DailyGoalType.XP)
                .goalKey("XP")
                .goalDate(LocalDate.now())
                .targetValue(100)
                .currentValue(50)
                .isCompleted(false)
                .build();
        when(dailyGoalRepository.findByUserIdAndGoalDateAndGoalKey("user-1", LocalDate.now(), "XP"))
                .thenReturn(Optional.of(goal));

        dailyGoalService.recordProgress("user-1", DailyGoalType.XP, 50);

        assertThat(goal.getCurrentValue()).isEqualTo(100);
        assertThat(goal.getIsCompleted()).isTrue();
        verify(dailyGoalRepository).save(goal);
        verify(userExperienceService).award("user-1", 50);
    }

    @Test
    void recordLessonCompletedOnlyUpdatesMatchingSpecificLessonGoal() {
        UserDailyGoal goal = UserDailyGoal.builder()
                .userId("user-1")
                .goalType(DailyGoalType.SPECIFIC_LESSON_COMPLETED)
                .goalKey("SPECIFIC_LESSON_COMPLETED:lesson-1")
                .goalDate(LocalDate.now())
                .targetItemId("lesson-1")
                .targetValue(1)
                .currentValue(0)
                .isCompleted(false)
                .build();
        when(dailyGoalRepository.findByUserIdAndGoalDateAndGoalTypeAndTargetItemId(
                "user-1",
                LocalDate.now(),
                DailyGoalType.SPECIFIC_LESSON_COMPLETED,
                "lesson-1"))
                .thenReturn(Optional.of(goal));

        dailyGoalService.recordLessonCompleted("user-1", "lesson-1");

        assertThat(goal.getCurrentValue()).isEqualTo(1);
        assertThat(goal.getIsCompleted()).isTrue();
        verify(dailyGoalRepository).save(goal);
    }
}
