package com.cinx.learning.learningpath.application;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.CourseImageView;
import com.cinx.learning.catalog.application.query.CourseView;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.query.VideoLessonInfo;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.learningpath.application.query.LearningPathItemView;
import com.cinx.learning.learningpath.application.query.LearningPathView;
import com.cinx.learning.studyplan.application.StudyPlanScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adds what the path page shows per item: the course title and cover, the lesson title, type and minutes, and
 * whether the learner has the course. One batched lookup per kind for the whole path.
 */
@Component
@RequiredArgsConstructor
public class LearningPathItemEnricher {
    private final CatalogLookup catalogLookup;
    private final ICourseService courseService;

    LearningPathView enrich(String userId, LearningPathView path, Set<String> entitledCourseIds) {
        List<LearningPathItemView> items = path.getItems();
        if (items == null || items.isEmpty()) {
            return path;
        }
        List<String> courseIds = items.stream().map(LearningPathItemView::getCourseId).distinct().toList();
        Map<String, CourseView> courses = courseService.getReadableCourseByIds(userId, courseIds).stream()
                .collect(Collectors.toMap(CourseView::id, Function.identity(), (a, b) -> a));
        // a lesson id on a path item is the lesson's stable id, as in progress
        Map<String, LessonInfo> lessons = catalogLookup.readableLessonsOfCourses(courseIds).stream()
                .collect(Collectors.toMap(LessonInfo::getStableId, Function.identity(), (a, b) -> a));
        List<String> videoIds = lessons.values().stream()
                .filter(lesson -> lesson.getLessonType() == LessonType.VIDEO)
                .map(LessonInfo::getStableId)
                .toList();
        Map<String, Integer> videoSeconds = videoIds.isEmpty() ? Map.of() : catalogLookup.videoLessons(videoIds).stream()
                .filter(video -> video.getDuration() != null)
                .collect(Collectors.toMap(VideoLessonInfo::getLessonId, VideoLessonInfo::getDuration, (a, b) -> a));

        for (LearningPathItemView item : items) {
            CourseView course = courses.get(item.getCourseId());
            if (course != null) {
                item.setCourseTitle(course.title());
                item.setCourseThumbnailUrl(thumbnail(course));
            }
            LessonInfo lesson = lessons.get(item.getLessonId());
            if (lesson != null) {
                item.setLessonTitle(lesson.getTitle());
                item.setLessonType(lesson.getLessonType());
                item.setDurationMinutes(StudyPlanScheduler.estimateMinutes(lesson.getLessonType(), videoSeconds.get(lesson.getStableId())));
            }
            item.setEntitled(entitledCourseIds.contains(item.getCourseId()));
        }
        return path;
    }

    private static String thumbnail(CourseView course) {
        return course.images() == null ? null : course.images().stream()
                .map(CourseImageView::imageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
