package com.cinx.learning.progress.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.courseview.application.query.LessonView;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.engagement.application.IStreakService;
import com.cinx.learning.enrollment.application.EnrollmentAccess;
import com.cinx.learning.learningpath.application.ILearningPathService;
import com.cinx.learning.notificationprojection.application.LearningNotifications;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.progress.application.mapper.CourseProgressMapper;
import com.cinx.learning.progress.application.mapper.LearningItemProgressMapper;
import com.cinx.learning.progress.domain.CourseProgress;
import com.cinx.learning.progress.domain.CourseProgressRepository;
import com.cinx.learning.progress.domain.LearningItemProgress;
import com.cinx.learning.progress.domain.LearningItemProgressRepository;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.wardrobe.application.IOwlWardrobeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningProgressServiceTest {
    @Mock
    private CourseProgressRepository courseProgressRepository;
    @Mock
    private LearningItemProgressRepository learningItemProgressRepository;
    @Mock
    private CourseProgressMapper courseProgressMapper;
    @Mock
    private LearningItemProgressMapper learningItemProgressMapper;
    @Mock
    private CourseService courseService;
    @Mock
    private ILearningPathService learningPathService;
    @Mock
    private IStreakService streakService;
    @Mock
    private IOwlWardrobeService owlWardrobeService;
    @Mock
    private IDailyGoalService dailyGoalService;
    @Mock
    private EnrollmentAccess enrollmentAccess;
    @Mock
    private LearningNotifications notificationPublisher;
    @Spy
    private CourseProgressCalculator courseProgressCalculator = new CourseProgressCalculator();

    @InjectMocks
    private LearningProgressService learningProgressService;

    @Test
    void completeArticleItemRejectsNonArticleLesson() {
        CourseProgress courseProgress = courseProgress("course-1", 1);
        LearningItemProgress progress = item("video-1", courseProgress, false, false, null);
        LessonView videoLesson = new LessonView("video-1", "Video", 120L, LessonType.VIDEO, 1);

        when(learningItemProgressRepository.findByItemIdAndUserIdForUpdate("video-1", "user-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.findById("cp-course-1")).thenReturn(Optional.of(courseProgress));
        when(courseService.getEnrolledLessonById("course-1", "video-1"))
                .thenReturn(new ApiResponse<>(true, "ok", videoLesson));

        assertThatThrownBy(() -> learningProgressService.completeArticleItem("user-1", "video-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only article lessons");
    }

    @Test
    void failedItemCompletesButDoesNotPassCourse() {
        CourseProgress courseProgress = courseProgress("course-1", 1);
        LearningItemProgress failedQuiz = item("quiz-1", courseProgress, false, false, null);

        when(learningItemProgressRepository.findByItemIdAndUserIdForUpdate("quiz-1", "user-1"))
                .thenReturn(Optional.of(failedQuiz));
        when(courseProgressRepository.findById("cp-course-1")).thenReturn(Optional.of(courseProgress));
        when(learningItemProgressRepository.findAllByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(List.of(failedQuiz));

        LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                "user-1",
                "quiz-1",
                new UpdateLearningItemCommand(true, false, 4.0));

        assertThat(result.completedTransition()).isTrue();
        assertThat(result.passedTransition()).isFalse();
        assertThat(result.courseCompletedTransition()).isTrue();
        assertThat(result.coursePassedTransition()).isFalse();
        assertThat(failedQuiz.getIsCompleted()).isTrue();
        assertThat(failedQuiz.getIsPassed()).isFalse();
        assertThat(failedQuiz.getScore()).isEqualTo(4.0);
        assertThat(courseProgress.getCompletedItems()).isEqualTo(1);
        assertThat(courseProgress.getAvgScore()).isEqualTo(4.0);
        assertThat(courseProgress.getIsCompleted()).isTrue();
        assertThat(courseProgress.getIsPassed()).isFalse();
    }

    @Test
    void failedItemLaterPassedDoesNotRepeatCompletionSideEffects() {
        CourseProgress courseProgress = courseProgress("course-1", 1);
        LearningItemProgress failedQuiz = item("quiz-1", courseProgress, true, false, 4.0);
        courseProgress.setCompletedItems(1);
        courseProgress.setIsCompleted(true);

        when(learningItemProgressRepository.findByItemIdAndUserIdForUpdate("quiz-1", "user-1"))
                .thenReturn(Optional.of(failedQuiz));
        when(courseProgressRepository.findById("cp-course-1")).thenReturn(Optional.of(courseProgress));
        when(learningItemProgressRepository.findAllByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(List.of(failedQuiz));

        LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                "user-1",
                "quiz-1",
                new UpdateLearningItemCommand(true, true, 7.0));

        assertThat(result.completedTransition()).isFalse();
        assertThat(result.passedTransition()).isTrue();
        assertThat(result.courseCompletedTransition()).isFalse();
        assertThat(result.coursePassedTransition()).isTrue();
        assertThat(failedQuiz.getIsCompleted()).isTrue();
        assertThat(failedQuiz.getIsPassed()).isTrue();
        assertThat(courseProgress.getCompletedItems()).isEqualTo(1);
        assertThat(courseProgress.getAvgScore()).isEqualTo(7.0);
        assertThat(courseProgress.getIsCompleted()).isTrue();
        assertThat(courseProgress.getIsPassed()).isTrue();
    }

    @Test
    void passingFinalItemCompletesAndPassesCourse() {
        CourseProgress courseProgress = courseProgress("course-1", 2);
        LearningItemProgress quiz = item("quiz-1", courseProgress, false, false, null);
        LearningItemProgress article = item("article-1", courseProgress, true, true, 8.0);

        when(learningItemProgressRepository.findByItemIdAndUserIdForUpdate("quiz-1", "user-1"))
                .thenReturn(Optional.of(quiz));
        when(courseProgressRepository.findById("cp-course-1")).thenReturn(Optional.of(courseProgress));
        when(learningItemProgressRepository.findAllByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(List.of(quiz, article));

        LearningItemProgressUpdateResult result = learningProgressService.updateLearningItemProgress(
                "user-1",
                "quiz-1",
                new UpdateLearningItemCommand(true, true, 10.0));

        assertThat(result.completedTransition()).isTrue();
        assertThat(result.passedTransition()).isTrue();
        assertThat(result.courseCompletedTransition()).isTrue();
        assertThat(result.coursePassedTransition()).isTrue();
        assertThat(quiz.getIsCompleted()).isTrue();
        assertThat(quiz.getIsPassed()).isTrue();
        assertThat(courseProgress.getCompletedItems()).isEqualTo(2);
        assertThat(courseProgress.getAvgScore()).isEqualTo(9.0);
        assertThat(courseProgress.getIsCompleted()).isTrue();
        assertThat(courseProgress.getIsPassed()).isTrue();
        assertThat(courseProgress.getCompletionTime()).isNotNull();
        verify(notificationPublisher).publishCourseCompleted("user-1", "course-1", "your course");
    }

    private CourseProgress courseProgress(String courseId, int totalItems) {
        CourseProgress courseProgress = new CourseProgress();
        courseProgress.setId("cp-" + courseId);
        courseProgress.setCourseId(courseId);
        courseProgress.setTotalItems(totalItems);
        courseProgress.setCompletedItems(0);
        courseProgress.setIsCompleted(false);
        courseProgress.setIsPassed(false);
        return courseProgress;
    }

    private LearningItemProgress item(String itemId, CourseProgress courseProgress, boolean completed, boolean passed, Double score) {
        LearningItemProgress progress = new LearningItemProgress();
        progress.setItemId(itemId);
        progress.setCourseProgressId(courseProgress.getId());
        progress.setIsCompleted(completed);
        progress.setIsPassed(passed);
        progress.setScore(score);
        return progress;
    }
}
