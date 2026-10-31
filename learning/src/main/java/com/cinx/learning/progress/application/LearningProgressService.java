package com.cinx.learning.progress.application;

import com.cinx.learning.progress.domain.CourseProgressAggregate;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.courseview.application.query.LessonView;
import com.cinx.learning.progress.application.query.LearningItemProgressView;
import com.cinx.learning.progress.application.mapper.CourseProgressMapper;
import com.cinx.learning.progress.application.mapper.LearningItemProgressMapper;
import com.cinx.learning.notificationprojection.application.LearningNotifications;
import com.cinx.learning.progress.domain.CourseProgress;
import com.cinx.learning.progress.domain.LearningItemProgress;
import com.cinx.learning.progress.domain.CourseProgressRepository;
import com.cinx.learning.progress.domain.LearningItemProgressRepository;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.enrollment.application.EnrollmentAccess;
import com.cinx.learning.learningpath.application.ILearningPathService;
import com.cinx.learning.engagement.application.IStreakService;
import com.cinx.learning.wardrobe.application.IOwlWardrobeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningProgressService implements ILearningProgressService {
    private final CourseProgressRepository courseProgressRepository;
    private final LearningItemProgressRepository learningItemProgressRepository;
    private final CourseProgressMapper courseProgressMapper;
    private final LearningItemProgressMapper learningItemProgressMapper;
    private final CourseService courseService;
    private final ILearningPathService learningPathService;
    private final IStreakService streakService;
    private final IOwlWardrobeService owlWardrobeService;
    private final IDailyGoalService dailyGoalService;
    private final EnrollmentAccess enrollmentAccess;
    private final CourseProgressCalculator courseProgressCalculator;

    private final LearningNotifications notificationPublisher;

    @Override
    public List<CourseProgressView> getCourseProgressByCourseIds(String userId, List<String> courseIds) {
        return courseProgressRepository.findAllByUserIdAndCourseIdIn(userId, courseIds)
                .stream()
                .map(courseProgressMapper::toDto)
                .toList();
    }

    @Override
    public CourseProgressView getCourseProgress(String userId, String courseId) {
        return courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                .map(courseProgressMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Course progress not found"));
    }

    @Override
    public List<LearningItemProgressView> getLearningItemProgressByCourseId(String userId, String courseId) {
        return learningItemProgressRepository.findAllByUserIdAndCourseId(userId, courseId)
                .stream()
                .map(learningItemProgressMapper::toDto)
                .toList();
    }

    @Override
    public List<CourseProgressView> getCourseProgressByCourseId(String courseId) {
        return courseProgressRepository.findAllByCourseId(courseId)
                .stream()
                .map(courseProgressMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public void createCourseProgress(String userId, String courseId) {
        if (courseProgressRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return;
        }
        List<String> lessonIds = courseService.getCourseLessonIdsByCourseId(courseId).data();
        CourseProgress courseProgress = courseProgressRepository.save(
                CourseProgress.builder()
                        .isCompleted(false)
                        .isPassed(false)
                        .avgScore(0.0)
                        .userId(userId)
                        .courseId(courseId)
                        .totalItems(lessonIds.size())
                        .completedItems(0)
                        .build()
        );
        learningItemProgressRepository.saveAll(lessonIds.stream()
                        .<LearningItemProgress>map(lessonId -> LearningItemProgress.builder()
                        .isCompleted(false)
                        .isPassed(false)
                        .courseProgressId(courseProgress.getId())
                        .itemId(lessonId)
                        .build())
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLearningItemCompleted(String userId, String itemId) {
        return learningItemProgressRepository
                .findByItemIdAndUserId(itemId, userId)
                .map(LearningItemProgress::getIsCompleted)
                .orElse(false);
    }

    @Transactional
    @Override
    public LearningItemProgressUpdateResult updateLearningItemProgress(String userId, String itemId, UpdateLearningItemCommand request) {
        LearningItemProgress progress = learningItemProgressRepository
                .findByItemIdAndUserIdForUpdate(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Learning item progress not found"));

        CourseProgress course = courseProgressOf(progress);
        boolean oldCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
        boolean oldPassed = Boolean.TRUE.equals(progress.getIsPassed());
        boolean wasCourseCompleted = Boolean.TRUE.equals(course.getIsCompleted());
        boolean wasCoursePassed = Boolean.TRUE.equals(course.getIsPassed());
        Boolean requestedCompleted = request.isCompleted() != null ? request.isCompleted() : oldCompleted;
        Boolean newPassed = request.isPassed() != null ? request.isPassed() : progress.getIsPassed();
        Double newScore = request.score() != null ? request.score() : progress.getScore();

        progress.setIsCompleted(Boolean.TRUE.equals(requestedCompleted));
        progress.setIsPassed(newPassed);
        progress.setScore(newScore);
        boolean completedTransition = !oldCompleted && Boolean.TRUE.equals(progress.getIsCompleted());
        if (completedTransition && progress.getCompletedAt() == null) {
            // stored in UTC explicitly (column contract); the study plan converts it to Asia/Ho_Chi_Minh days
            progress.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        learningItemProgressRepository.save(progress);

        boolean passedTransition = !oldPassed && Boolean.TRUE.equals(progress.getIsPassed());

        if (completedTransition) {
            streakService.updateStreakOnActivity(userId);
            owlWardrobeService.grantOpenSeasonSkins(userId);
            dailyGoalService.recordProgress(userId, DailyGoalType.LEARNING_ITEMS_COMPLETED, 1);
            dailyGoalService.recordProgress(userId, DailyGoalType.XP, 50);
            dailyGoalService.recordLessonCompleted(userId, itemId);
            learningPathService.updatePathProgress(userId, itemId);
        }

        CourseProgressAggregate aggregate = recomputeCourseAggregate(course, userId);
        boolean courseCompletedTransition = !wasCourseCompleted && aggregate.completed();
        boolean coursePassedTransition = !wasCoursePassed && aggregate.passed();
        if (coursePassedTransition) {
            publishCourseCompleted(userId, course.getCourseId());
        }
        return new LearningItemProgressUpdateResult(
                completedTransition,
                passedTransition,
                courseCompletedTransition,
                coursePassedTransition);
    }

    @Transactional
    @Override
    public void completeArticleItem(String userId, String itemId) {
        LearningItemProgress progress = learningItemProgressRepository
                .findByItemIdAndUserIdForUpdate(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Learning item progress not found"));
        String courseId = courseProgressOf(progress).getCourseId();
        LessonView lesson = courseService.getEnrolledLessonById(courseId, itemId).data();
        if (lesson == null || lesson.lessonType() != LessonType.ARTICLE) {
            throw new BadRequestException(ErrorCode.LESSON_TYPE_INVALID, "Only article lessons can be manually marked complete");
        }
        updateLearningItemProgress(userId, itemId, new UpdateLearningItemCommand(true, true, 10.0));
    }

    @Transactional
    @Override
    public void recomputeCourseProgress(String courseId) {
        log.info("Recomputing course progress for courseId={}", courseId);

        // 1. Fetch the current, authoritative lesson set from the course service.
        List<String> currentLessonIds;
        try {
            currentLessonIds = courseService.getCourseLessonIdsByCourseId(courseId).data();
        } catch (Exception ex) {
            log.error("Cannot fetch course detail for courseId={}, aborting recompute", courseId, ex);
            return;
        }

        int expectedTotal = currentLessonIds.size();

        // 2. Get all enrolled users for this course.
        List<String> enrolledUserIds = enrollmentAccess.userIdsOfCourse(courseId);
        if (enrolledUserIds.isEmpty()) {
            try {
                enrolledUserIds = enrollmentAccess.userIdsOfCourse(courseId);
            } catch (Exception ex) {
                log.error("Cannot fetch enrolled users for courseId={}, aborting recompute", courseId, ex);
                return;
            }
        }

        if (enrolledUserIds == null || enrolledUserIds.isEmpty()) {
            log.info("No enrolled users for courseId={}, nothing to recompute", courseId);
            return;
        }

        // 3. Recompute each user's progress.
        for (String userId : enrolledUserIds) {
            try {
                recomputeForUser(userId, courseId, currentLessonIds, expectedTotal);
                learningPathService.refreshPrerequisiteUnlocks(userId, courseId);
            } catch (Exception ex) {
                log.error("Failed to recompute progress for userId={} courseId={}", userId, courseId, ex);
                // Continue with next user — don't fail the entire batch.
            }
        }
    }

    private void recomputeForUser(String userId, String courseId,
                                  List<String> currentLessonIds, int expectedTotal) {
        CourseProgress courseProgress = courseProgressRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (courseProgress == null) {
            log.debug("No CourseProgress for userId={} courseId={}, skipping", userId, courseId);
            return;
        }

        // Existing item-progress rows for this user/course.
        List<LearningItemProgress> existingItems =
                learningItemProgressRepository.findAllByUserIdAndCourseId(userId, courseId);

        Map<String, LearningItemProgress> existingByItemId = existingItems.stream()
                .collect(Collectors.toMap(LearningItemProgress::getItemId, i -> i, (first, duplicate) -> first));

        Set<String> currentSet = new HashSet<>(currentLessonIds);

        // Remove stale rows (deleted lessons).
        List<LearningItemProgress> toDelete = existingItems.stream()
                .filter(item -> !currentSet.contains(item.getItemId()))
                .toList();
        if (!toDelete.isEmpty()) {
            learningItemProgressRepository.deleteAll(toDelete);
            log.debug("Removed {} stale item-progress row(s) for userId={} courseId={}",
                    toDelete.size(), userId, courseId);
        }

        // Create missing rows (new lessons).
        List<LearningItemProgress> toAdd = currentLessonIds.stream()
                .filter(lid -> !existingByItemId.containsKey(lid))
                .<LearningItemProgress>map(lid -> LearningItemProgress.builder()
                        .itemId(lid)
                        .isCompleted(false)
                        .isPassed(false)
                        .courseProgressId(courseProgress.getId())
                        .build())
                .toList();
        if (!toAdd.isEmpty()) {
            learningItemProgressRepository.saveAll(toAdd);
            log.debug("Added {} new item-progress row(s) for userId={} courseId={}",
                    toAdd.size(), userId, courseId);
        }

        List<LearningItemProgress> currentItems =
                learningItemProgressRepository.findAllByUserIdAndCourseId(userId, courseId);

        List<LearningItemProgress> currentCourseItems = currentItems.stream()
                .filter(item -> currentSet.contains(item.getItemId()))
                .toList();
        boolean wasPassed = Boolean.TRUE.equals(courseProgress.getIsPassed());
        CourseProgressAggregate aggregate = courseProgressCalculator.calculate(currentCourseItems, expectedTotal);
        applyCourseAggregate(courseProgress, aggregate);
        boolean justPassed = aggregate.passed() && !wasPassed;

        if (aggregate.completed() && courseProgress.getCompletionTime() == null) {
            courseProgress.setCompletionTime(LocalDateTime.now());
        } else if (!aggregate.completed()) {
            courseProgress.setCompletionTime(null);
        }

        courseProgressRepository.save(courseProgress);

        if (justPassed) {
            publishCourseCompleted(userId, courseId);
        }
    }

    private CourseProgressAggregate recomputeCourseAggregate(CourseProgress course, String userId) {
        List<LearningItemProgress> items =
                learningItemProgressRepository.findAllByUserIdAndCourseId(userId, course.getCourseId());
        CourseProgressAggregate aggregate = courseProgressCalculator.calculate(items, course.getTotalItems());
        applyCourseAggregate(course, aggregate);
        if (aggregate.completed() && course.getCompletionTime() == null) {
            course.setCompletionTime(LocalDateTime.now());
        } else if (!aggregate.completed()) {
            course.setCompletionTime(null);
        }

        courseProgressRepository.save(course);
        return aggregate;
    }

    private CourseProgress courseProgressOf(LearningItemProgress progress) {
        return courseProgressRepository.findById(progress.getCourseProgressId())
                .orElseThrow(() -> new NotFoundException("Course progress not found"));
    }

    private void applyCourseAggregate(CourseProgress course, CourseProgressAggregate aggregate) {
        course.setTotalItems(aggregate.totalItems());
        course.setCompletedItems(aggregate.completedItems());
        course.setAvgScore(aggregate.avgScore());
        course.setIsCompleted(aggregate.completed());
        course.setIsPassed(aggregate.passed());
    }

    private void publishCourseCompleted(String userId, String courseId) {
        try {
            String courseTitle = "your course";
            var courseRes = courseService.getCourseById(courseId);
            if (courseRes != null && courseRes.success() && courseRes.data() != null) {
                courseTitle = courseRes.data().title();
            }

            notificationPublisher.publishCourseCompleted(userId, courseId, courseTitle);
        } catch (Exception ex) {
            log.error("Failed to publish course completion event for userId={}, courseId={}", userId, courseId, ex);
        }
    }

    // Course content notifications are handled end-to-end by the notification service.
}
