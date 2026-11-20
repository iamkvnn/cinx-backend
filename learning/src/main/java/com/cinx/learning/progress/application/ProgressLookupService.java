package com.cinx.learning.progress.application;

import com.cinx.learning.progress.application.mapper.CourseProgressMapper;
import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import com.cinx.learning.progress.domain.CourseProgressRepository;
import com.cinx.learning.progress.domain.LearningItemProgressRepository;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistory;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressLookupService implements ProgressLookup {
    private final CourseProgressRepository courseProgressRepository;
    private final CourseProgressMapper courseProgressMapper;
    private final LearningItemProgressRepository itemProgressRepository;
    private final VideoLessonTrackingHistoryRepository trackingRepository;

    @Override
    public List<CourseProgressInfo> courseProgressOfUser(String userId) {
        return courseProgressRepository.findAllByUserId(userId).stream().map(CourseProgressInfo::of).toList();
    }

    @Override
    public List<CourseProgressInfo> courseProgressOfUser(String userId, List<String> courseIds) {
        return courseProgressRepository.findAllByUserIdAndCourseIdIn(userId, courseIds).stream().map(CourseProgressInfo::of).toList();
    }

    @Override
    public List<CourseProgressView> courseProgressViewsOfUser(String userId, List<String> courseIds) {
        return courseProgressRepository.findAllByUserIdAndCourseIdIn(userId, courseIds).stream().map(courseProgressMapper::toDto).toList();
    }

    @Override
    public List<CourseProgressInfo> courseProgressOfCourse(String courseId) {
        return courseProgressRepository.findAllByCourseId(courseId).stream().map(CourseProgressInfo::of).toList();
    }

    @Override
    public List<CourseProgressInfo> courseProgressOfCourses(List<String> courseIds) {
        return courseProgressRepository.findAllByCourseIdIn(courseIds).stream().map(CourseProgressInfo::of).toList();
    }

    @Override
    public List<LearningItemProgressInfo> itemProgressOfUser(String userId, String courseId) {
        return itemProgressRepository.findAllByUserIdAndCourseId(userId, courseId).stream().map(LearningItemProgressInfo::of).toList();
    }

    @Override
    public List<LearningItemProgressInfo> itemProgressOfUser(String userId, List<String> itemIds) {
        return itemProgressRepository.findAllByUserIdAndItemIdIn(userId, itemIds).stream().map(LearningItemProgressInfo::of).toList();
    }

    @Override
    public Optional<LearningItemProgressInfo> itemProgress(String itemId, String userId) {
        return itemProgressRepository.findByItemIdAndUserId(itemId, userId).map(LearningItemProgressInfo::of);
    }

    @Override
    public List<CompletionRow> completionPerCourse(Collection<String> courseIds) {
        return courseProgressRepository.completionPerCourse(courseIds).stream()
                .map(row -> new CompletionRow(row.getCourseId(), row.getTotal(), row.getCompleted()))
                .toList();
    }

    @Override
    public List<ItemCount> completedPerItem(String courseId) {
        return itemProgressRepository.completedPerItem(courseId).stream()
                .map(row -> new ItemCount(row.getItemId(), row.getTotal()))
                .toList();
    }

    @Override
    public List<PassedRow> passedIn(String courseId, Collection<String> itemIds) {
        return itemProgressRepository.passedIn(courseId, itemIds).stream()
                .map(row -> new PassedRow(row.getItemId(), row.getUserId()))
                .toList();
    }

    @Override
    public Optional<Integer> videoPosition(String userId, String videoLessonId) {
        return trackingRepository.findByUserIdAndVideoLessonId(userId, videoLessonId).map(VideoLessonTrackingHistory::getCurrentPosition);
    }
}
