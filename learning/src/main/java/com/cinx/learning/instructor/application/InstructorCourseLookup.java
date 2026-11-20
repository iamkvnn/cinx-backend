package com.cinx.learning.instructor.application;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.application.query.CourseImageInfo;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.CurriculumLookup;
import com.cinx.learning.catalog.application.query.SectionPlace;
import com.cinx.learning.catalog.application.ICourseAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The courses an instructor may act on (MANAGER or higher, the same bar as grading and answering), and the readable
 * lessons behind the stable ids that submissions, questions and quizzes carry.
 */
@Component
@RequiredArgsConstructor
public class InstructorCourseLookup {
    private final CatalogLookup catalogLookup;
    private final CurriculumLookup curriculum;
    private final ICourseAccessService courseAccessService;

    /** A course as the inbox and overview show it. */
    public record CourseCard(String id, String title, String thumbnailUrl, String status, String publishStatus, Double rating) {
    }

    /** A readable lesson, by its stable id, with what the inbox shows about it. */
    public record LessonRef(String stableId, String lessonId, String title, String sectionTitle, String courseId,
                            com.cinx.learning.shared.domain.LessonType type) {
    }

    @Transactional(readOnly = true)
    public List<CourseCard> managedCourses(String userId) {
        List<String> ids = catalogLookup.courseIdsOfInstructor(userId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return catalogLookup.courses(ids).stream()
                .filter(course -> courseAccessService.hasCourseCapability(userId, course, CourseMembershipRole.MANAGER))
                .map(course -> new CourseCard(
                        course.getId(),
                        course.getTitle(),
                        thumbnail(course),
                        course.getStatus() == null ? null : course.getStatus().name(),
                        course.getPublishStatus() == null ? null : course.getPublishStatus().name(),
                        course.getRating()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, LessonRef> lessonsByStableId(Collection<String> stableIds) {
        if (stableIds == null || stableIds.isEmpty()) {
            return Map.of();
        }
        return refs(catalogLookup.readableLessonsByStableIds(stableIds)).stream()
                .collect(Collectors.toMap(LessonRef::stableId, ref -> ref, (a, b) -> a));
    }

    /** The readable lessons of these courses, keyed by stable id. */
    @Transactional(readOnly = true)
    public Map<String, LessonRef> lessonsOfCourses(Collection<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        return refs(catalogLookup.readableLessonsOfCourses(courseIds)).stream()
                .collect(Collectors.toMap(LessonRef::stableId, ref -> ref, (a, b) -> a));
    }

    public static String thumbnail(CourseInfo course) {
        List<CourseImageInfo> images = course.getImages();
        if (images == null) {
            return null;
        }
        return images.stream().map(CourseImageInfo::getImageUrl).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<LessonRef> refs(List<LessonInfo> lessons) {
        Map<String, SectionPlace> places = curriculum.sectionPlaces(
                lessons.stream().map(LessonInfo::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return lessons.stream().map(lesson -> toRef(lesson, places.get(lesson.getSectionId()))).toList();
    }

    private static LessonRef toRef(LessonInfo lesson, SectionPlace place) {
        return new LessonRef(
                lesson.getStableId(),
                lesson.getId(),
                lesson.getTitle(),
                place.title(),
                place.courseId(),
                lesson.getLessonType());
    }
}
