package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.command.CreateLessonCommand;
import com.cinx.learning.catalog.application.command.MoveLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateLessonCommand;
import com.cinx.learning.catalog.application.query.LessonPositionView;
import com.cinx.learning.catalog.application.query.LessonView;
import com.cinx.learning.catalog.application.mapper.LessonMapper;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinx.learning.progress.application.IPrerequisiteService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LessonService implements ILessonService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final SectionRepository sectionRepository;
    private final ICourseDraftService courseDraftService;
    private final ISectionService sectionService;
    private final LessonMapper lessonMapper;
    private final IPrerequisiteService prerequisiteService;
    private final ICourseAccessService courseAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<String> getLessonIdsByCourseId(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new NotFoundException("Course not found with id: " + courseId);
        }
        return lessonRepository.findPublishedByCourse(courseId)
                .stream()
                .map(Lesson::getStableId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getEnrolledLessonIdsByCourseId(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED && course.getStatus() != CourseStatus.ARCHIVED) {
            throw new NotFoundException("Course not found with id: " + courseId);
        }
        return lessonRepository.findEnrolledReadableByCourse(courseId)
                .stream()
                .map(Lesson::getStableId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LessonView getEnrolledLessonByCourseIdAndLessonId(String courseId, String lessonId) {
        return findEnrolledReadableLesson(courseId, lessonId)
                .map(lessonMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Lesson not found with id: " + lessonId));
    }

    @Override
    @Transactional(readOnly = true)
    public LessonView getUserReadableLessonByCourseIdAndLessonId(String currentUserId,
                                                                       String courseId,
                                                                       String lessonId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (!courseAccessService.canReadCourse(currentUserId, course)) {
            throw new NotFoundException("Lesson not found with id: " + lessonId);
        }
        return findEnrolledReadableLesson(courseId, lessonId)
                .map(lessonMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Lesson not found with id: " + lessonId));
    }

    @Override
    @Transactional
    public Lesson getForUpdate(String courseId, String sectionId, String lessonId, LessonType lessonType) {
        Section section = sectionService.editableSection(courseId, sectionId);
        Lesson lesson = lessonRepository.findBySectionAndStableId(section.getId(), lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found with id: " + lessonId));
        lesson.setSectionId(section.getId());
        if (lessonType != null && lesson.getLessonType() != lessonType) {
            throw new NotFoundException("Lesson not found with id: " + lessonId);
        }
        return lesson;
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureLessonBelongsToCourse(String courseId, String lessonId, LessonType lessonType) {
        List<Lesson> lessons = lessonRepository.findByCourseAndStableId(courseId, lessonId);
        if (lessons.isEmpty()) {
            throw new NotFoundException("Lesson not found with id: " + lessonId);
        }
        if (lessonType != null && lessons.stream().noneMatch(lesson -> lesson.getLessonType() == lessonType)) {
            throw new NotFoundException("Lesson not found with id: " + lessonId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureCanReadLessonContent(String currentUserId, String courseId, String lessonId, LessonType lessonType) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (courseAccessService.canReadCourse(currentUserId, course)) {
            Optional<Lesson> readableLesson = lessonRepository.findReadableByCourseAndStableId(courseId, lessonId).stream()
                    .filter(candidate -> lessonType == null || candidate.getLessonType() == lessonType)
                    .findFirst();
            if (readableLesson.isPresent()) {
                ensureReadableLessonAccess(currentUserId, courseId, course, readableLesson.get());
                return;
            }
        }

        // The public course page: a published course's preview lessons are open to everyone, buyers-to-be included
        if (course.getStatus() == CourseStatus.PUBLISHED && lessonRepository.findReadableByCourseAndStableId(courseId, lessonId).stream()
                .filter(candidate -> lessonType == null || candidate.getLessonType() == lessonType)
                .anyMatch(candidate -> Boolean.TRUE.equals(candidate.getIsPreview()))) {
            return;
        }

        if (!courseAccessService.canManageCourse(currentUserId, course)) {
            throw new NotFoundException("Lesson not found with id: " + lessonId);
        }
        lessonRepository.findByCourseAndStableId(courseId, lessonId).stream()
                .filter(candidate -> lessonType == null || candidate.getLessonType() == lessonType)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Lesson not found with id: " + lessonId));
    }

    private void ensureReadableLessonAccess(String currentUserId, String courseId, Course course, Lesson lesson) {
        if (Boolean.TRUE.equals(lesson.getIsPreview())) {
            return;
        }

        if (currentUserId == null) {
            throw new ForbiddenException(ErrorCode.NOT_ENROLLED_IN_COURSE, "You must enroll in this course before viewing this lesson");
        }
        if (courseAccessService.isAdmin() || courseAccessService.isActiveMember(currentUserId, course)) {
            return;
        }
        if (!courseAccessService.isEnrolled(currentUserId, courseId)
                && !courseAccessService.hasSubscriptionAccess(currentUserId, courseId)) {
            throw new ForbiddenException(ErrorCode.NOT_ENROLLED_IN_COURSE, "You must enroll in this course before viewing this lesson");
        }
        // prerequisites lock the lesson for learners (previews, admins and course members returned above)
        if (!lesson.getPrerequisiteIds().isEmpty()) {
            List<String> missing = prerequisiteService.missing(currentUserId, courseId, lesson.getStableId());
            if (!missing.isEmpty()) {
                throw new ForbiddenException(ErrorCode.LESSON_LOCKED, "Finish the prerequisite lessons first",
                        Map.of("missingPrerequisiteIds", missing));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureLessonInstructor(String currentUserId, String courseId, String lessonId, LessonType lessonType) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        ensureLessonBelongsToCourse(courseId, lessonId, lessonType);
    }

    private Optional<Lesson> findEnrolledReadableLesson(String courseId, String lessonId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED && course.getStatus() != CourseStatus.ARCHIVED) {
            throw new NotFoundException("Course not found with id: " + courseId);
        }
        return lessonRepository.findEnrolledReadableByCourse(courseId)
                .stream()
                .filter(lesson -> Objects.equals(lesson.getStableId(), lessonId))
                .findFirst();
    }


    @Override
    @Transactional
    public LessonView createLesson(String currentUserId, String courseId, String sectionId, CreateLessonCommand request) {
        ensureCourseInstructor(currentUserId, courseId);
        Section section = sectionService.editableSection(courseId, sectionId);
        String stableId = UUID.randomUUID().toString();
        List<String> prerequisiteIds = normalizePrerequisites(section.getDraftId(), stableId, request.prerequisiteIds());
        Lesson lesson = lessonMapper.toModel(request);
        lesson.setSectionId(section.getId());
        lesson.setStableId(stableId);
        lesson.setOrderIndex(nextLessonOrderIndex(section.getId()));
        lesson.setPrerequisiteIds(new ArrayList<>(prerequisiteIds));
        validatePreviewEligibility(lesson.getLessonType(), lesson.getIsPreview());
        lessonRepository.save(lesson);
        if (Boolean.TRUE.equals(request.aiAssisted())) {
            courseDraftService.markAiAssisted(courseRepository.findById(courseId)
                    .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId)));
        }
        return lessonMapper.toResponse(lesson);
    }

    @Override
    @Transactional
    public LessonView updateLesson(String currentUserId, String courseId, String sectionId, String lessonId, UpdateLessonCommand request) {
        ensureCourseInstructor(currentUserId, courseId);
        Lesson lesson = getForUpdate(courseId, sectionId, lessonId, null);
        lessonMapper.partialUpdate(lesson, request);
        if (request.prerequisiteIds() != null) {
            lesson.setPrerequisiteIds(new ArrayList<>(normalizePrerequisites(draftIdOf(lesson), lesson.getStableId(), request.prerequisiteIds())));
        }
        validatePreviewEligibility(lesson.getLessonType(), lesson.getIsPreview());
        lessonRepository.save(lesson);
        return lessonMapper.toResponse(lesson);
    }

    @Override
    @Transactional
    public LessonPositionView moveLesson(String currentUserId, String courseId, String lessonId, MoveLessonCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        CourseDraft draft = courseDraftService.getOrCreateDraft(course);
        List<Section> draftSections = sectionRepository.findDraftByDraftForUpdate(draft.getId());
        Map<String, Section> sectionsByStableId = draftSections.stream()
                .collect(Collectors.toMap(Section::getStableId, Function.identity()));
        Section targetSection = sectionsByStableId.get(request.targetSectionId());
        if (targetSection == null) {
            throw new BadRequestException(ErrorCode.LESSON_ORDER_INVALID, "Target section does not belong to this course draft: " + request.targetSectionId());
        }
        Lesson movedLesson = lessonRepository.findDraftLessonForUpdate(draft.getId(), lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found with id: " + lessonId));
        Section sourceSection = sectionRepository.findById(movedLesson.getSectionId())
                .orElseThrow(() -> new NotFoundException("Section not found with id: " + movedLesson.getSectionId()));

        List<String> affectedSectionIds = Stream.of(sourceSection, targetSection)
                .map(Section::getId)
                .distinct()
                .toList();
        List<Lesson> affectedLessons = lessonRepository.findBySectionIdsForUpdate(affectedSectionIds);
        List<Lesson> targetLessons = affectedLessons.stream()
                .filter(lesson -> Objects.equals(lesson.getSectionId(), targetSection.getId()))
                .filter(lesson -> !Objects.equals(lesson.getStableId(), lessonId))
                .collect(Collectors.toCollection(ArrayList::new));
        int targetIndex = OrderIndexUtils.insertionIndex(
                targetLessons,
                request.previousLessonId(),
                request.nextLessonId(),
                Lesson::getStableId,
                lessonId,
                "lesson"
        );
        Integer newOrderIndex = OrderIndexUtils.midpointOrderIndex(
                targetLessons,
                targetIndex,
                Lesson::getOrderIndex
        );
        boolean sectionChanged = !Objects.equals(sourceSection.getStableId(), targetSection.getStableId());
        if (newOrderIndex != null) {
            boolean orderChanged = !Objects.equals(movedLesson.getOrderIndex(), newOrderIndex);
            if (sectionChanged || orderChanged) {
                movedLesson.setSectionId(targetSection.getId());
                movedLesson.setOrderIndex(newOrderIndex);
                lessonRepository.save(movedLesson);
            }
            return new LessonPositionView(movedLesson.getStableId(), targetSection.getStableId(), movedLesson.getOrderIndex());
        }

        List<Lesson> desiredTargetLessons = new ArrayList<>(targetLessons);
        desiredTargetLessons.add(targetIndex, movedLesson);
        movedLesson.setSectionId(targetSection.getId());
        List<Lesson> changedLessons = OrderIndexUtils.rebalance(
                desiredTargetLessons,
                Lesson::getOrderIndex,
                Lesson::setOrderIndex
        );
        if (sectionChanged && !changedLessons.contains(movedLesson)) {
            changedLessons.add(movedLesson);
        }
        if (!changedLessons.isEmpty()) {
            lessonRepository.saveAll(changedLessons);
        }
        return new LessonPositionView(movedLesson.getStableId(), targetSection.getStableId(), movedLesson.getOrderIndex());
    }

    @Override
    @Transactional
    public void deleteLesson(String currentUserId, String courseId, String sectionId, String lessonId) {
        ensureCourseInstructor(currentUserId, courseId);
        Lesson lesson = getForUpdate(courseId, sectionId, lessonId, null);
        lessonRepository.delete(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLessonInstructor(String lessonId, String userId) {
        if (userId == null) {
            return false;
        }
        return lessonRepository.isAccessibleByInstructor(lessonId, userId);
    }

    private Integer nextLessonOrderIndex(String sectionId) {
        return lessonRepository.findBySectionIdsForUpdate(List.of(sectionId)).stream()
                .map(Lesson::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + OrderIndexUtils.ORDER_STEP;
    }

    private String draftIdOf(Lesson lesson) {
        return sectionRepository.findById(lesson.getSectionId())
                .map(Section::getDraftId)
                .orElseThrow(() -> new NotFoundException("Section not found with id: " + lesson.getSectionId()));
    }

    private void ensureCourseInstructor(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
    }

    private void validatePreviewEligibility(LessonType lessonType, Boolean isPreview) {
        if (!Boolean.TRUE.equals(isPreview)) {
            return;
        }
        if (lessonType != LessonType.ARTICLE && lessonType != LessonType.VIDEO) {
            throw new BadRequestException(ErrorCode.LESSON_TYPE_INVALID, "Only article or video lessons can be marked as preview");
        }
    }

    private List<String> normalizePrerequisites(String draftId, String targetStableId, List<String> prerequisiteIds) {
        List<String> normalized = prerequisiteIds == null
                ? List.of()
                : prerequisiteIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (normalized.contains(targetStableId)) {
            throw new BadRequestException(ErrorCode.LESSON_PREREQUISITE_INVALID, "Lesson cannot be a prerequisite of itself");
        }

        Map<String, Lesson> lessonsByStableId = lessonRepository.findByDraft(draftId).stream()
                .collect(Collectors.toMap(Lesson::getStableId, Function.identity(), (first, ignored) -> first));
        for (String prerequisiteId : normalized) {
            if (!lessonsByStableId.containsKey(prerequisiteId)) {
                throw new BadRequestException(ErrorCode.LESSON_PREREQUISITE_INVALID, "Prerequisite lesson not found: " + prerequisiteId);
            }
        }

        Map<String, List<String>> prerequisiteGraph = new HashMap<>();
        lessonsByStableId.values().forEach(lesson -> prerequisiteGraph.put(
                lesson.getStableId(),
                lesson.getPrerequisiteIds() == null ? List.of() : lesson.getPrerequisiteIds()
        ));
        prerequisiteGraph.put(targetStableId, normalized);

        for (String prerequisiteId : normalized) {
            if (canReach(prerequisiteId, targetStableId, prerequisiteGraph, new HashSet<>())) {
                throw new BadRequestException(ErrorCode.LESSON_PREREQUISITE_CYCLE, "Circular lesson prerequisite is not allowed");
            }
        }
        return normalized;
    }

    private boolean canReach(String current, String target, Map<String, List<String>> graph, Set<String> visited) {
        if (Objects.equals(current, target)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        return graph.getOrDefault(current, List.of()).stream()
                .anyMatch(next -> canReach(next, target, graph, visited));
    }
}

