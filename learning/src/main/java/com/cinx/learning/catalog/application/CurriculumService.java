package com.cinx.learning.catalog.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.CourseCurriculumView;
import com.cinx.learning.catalog.application.query.CurriculumSectionView;
import com.cinx.learning.catalog.application.query.LessonView;
import com.cinx.learning.catalog.application.mapper.LessonMapper;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SectionRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurriculumService implements ICurriculumService {
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final ICourseDraftService courseDraftService;
    private final ICourseAccessService courseAccessService;
    private final LessonMapper lessonMapper;

    @Override
    @Transactional(readOnly = true)
    public CourseCurriculumView getReadableCurriculum(String currentUserId, String courseId) {
        return publishedSnapshot(courseAccessService.ensureReadableCourse(currentUserId, courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCurriculumView getPublicCurriculum(String currentUserId, String courseId) {
        return publishedSnapshot(courseAccessService.ensureViewableCourse(currentUserId, courseId));
    }

    private CourseCurriculumView publishedSnapshot(Course course) {
        List<Section> sections = sectionRepository.findPublishedByCourse(course.getId());
        List<Lesson> lessons = lessonRepository.findPublishedByCourse(course.getId());
        return withVideoDurations(toResponse(course.getId(), sections, lessons), lessons);
    }

    /** Lesson.duration is often empty; a video lesson's real length lives on its VideoLesson. */
    private CourseCurriculumView withVideoDurations(CourseCurriculumView curriculum, List<Lesson> lessons) {
        List<String> videoIds = lessons.stream()
                .filter(lesson -> lesson.getLessonType() == LessonType.VIDEO)
                .map(Lesson::getStableId)
                .toList();
        if (videoIds.isEmpty()) {
            return curriculum;
        }
        Map<String, Integer> seconds = videoLessonRepository.findAllByLessonIdIn(videoIds).stream()
                .filter(video -> video.getDuration() != null)
                .collect(Collectors.toMap(VideoLesson::getLessonId, VideoLesson::getDuration, (a, b) -> a));
        List<CurriculumSectionView> sections = curriculum.sections().stream()
                .map(section -> new CurriculumSectionView(
                        section.id(),
                        section.title(),
                        section.description(),
                        section.duration(),
                        section.orderIndex(),
                        section.lessons().stream()
                                .map(lesson -> lesson.lessonType() == LessonType.VIDEO
                                        ? lesson.withVideoDurationSeconds(seconds.get(lesson.id()))
                                        : lesson)
                                .toList()))
                .toList();
        return new CourseCurriculumView(curriculum.courseId(), sections);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCurriculumView getEnrolledCurriculum(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED && course.getStatus() != CourseStatus.ARCHIVED) {
            throw new NotFoundException("Course not found with id: " + courseId);
        }
        List<Section> sections = sectionRepository.findPublishedByCourse(course.getId());
        List<Lesson> lessons = lessonRepository.findEnrolledReadableByCourse(course.getId());
        return toResponse(course.getId(), sections, lessons);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCurriculumView getEditableDraftCurriculum(String currentUserId, String courseId) {
        Course course = courseAccessService.ensureManageableCourse(currentUserId, courseId);
        return draftCurriculum(course);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCurriculumView getDraftCurriculumForReview(String courseId) {
        return draftCurriculum(courseAccessService.reviewableCourse(courseId));
    }

    private CourseCurriculumView draftCurriculum(Course course) {
        Optional<CourseDraft> draft = courseDraftService.findDraft(course);
        if (draft.isEmpty()) {
            if (course.getStatus() == CourseStatus.DRAFT) {
                return new CourseCurriculumView(course.getId(), List.of());
            }
            throw new NotFoundException("Course draft not found for course id: " + course.getId());
        }
        List<Section> sections = sectionRepository.findDraftByDraft(draft.get().getId());
        List<Lesson> lessons = lessonRepository.findDraftByDraft(draft.get().getId());
        return toResponse(course.getId(), sections, lessons);
    }

    private CourseCurriculumView toResponse(String courseId, List<Section> sections, List<Lesson> lessons) {
        Map<String, List<LessonView>> lessonsBySectionId = lessons.stream()
                .collect(Collectors.groupingBy(
                        Lesson::getSectionId,
                        LinkedHashMap::new,
                        Collectors.mapping(lessonMapper::toResponse, Collectors.toList())
                ));
        List<CurriculumSectionView> sectionResponses = sections.stream()
                .map(section -> new CurriculumSectionView(
                        section.getStableId(),
                        section.getTitle(),
                        section.getDescription(),
                        section.getDuration(),
                        section.getOrderIndex(),
                        lessonsBySectionId.getOrDefault(section.getId(), List.of())
                ))
                .toList();
        return new CourseCurriculumView(courseId, sectionResponses);
    }
}

