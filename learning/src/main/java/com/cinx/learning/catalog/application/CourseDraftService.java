package com.cinx.learning.catalog.application;

import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.application.command.UpdateCourseCommand;
import com.cinx.learning.catalog.application.mapper.CourseMapper;
import com.cinx.learning.catalog.application.mapper.LessonMapper;
import com.cinx.learning.catalog.application.mapper.SectionMapper;
import com.cinx.learning.catalog.application.event.LessonChangedEvent;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.CourseDraftRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseDraftService implements ICourseDraftService {
    private final CourseDraftRepository courseDraftRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final SectionMapper sectionMapper;
    private final LessonMapper lessonMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseDraft> findDraft(Course course) {
        return courseDraftRepository.findByCourse(course.getId());
    }

    @Override
    @Transactional
    public CourseDraft getOrCreateDraft(Course course) {
        return findDraft(course).orElseGet(() -> {
            course.setPublishStatus(null);
            courseRepository.save(course);
            return clonePublishedToDraft(course);
        });
    }

    @Override
    @Transactional
    public CourseDraft updateDraft(Course course, UpdateCourseCommand request, Category category, Long discountRate) {
        CourseDraft draft = getOrCreateDraft(course);
        courseMapper.partialUpdate(draft, request);
        // the update mapper skips nulls; an empty discount field must clear the discount
        draft.setDiscountedPrice(request.discountedPrice());
        draft.setCategoryId(category.getId());
        draft.setDiscountRate(discountRate);
        if (Boolean.TRUE.equals(request.aiAssisted())) {
            draft.setAiAssisted(true);
        }
        return courseDraftRepository.save(draft);
    }

    /** Turns the AI label on for the draft, and for the course too while it is not published, as updateCourse does. */
    @Override
    @Transactional
    public void markAiAssisted(Course course) {
        CourseDraft draft = getOrCreateDraft(course);
        if (!Boolean.TRUE.equals(draft.getAiAssisted())) {
            draft.setAiAssisted(true);
            courseDraftRepository.save(draft);
        }
        if (course.getStatus() != CourseStatus.PUBLISHED && !Boolean.TRUE.equals(course.getAiAssisted())) {
            course.setAiAssisted(true);
            courseRepository.save(course);
        }
    }

    @Override
    @Transactional
    public List<LessonChangedEvent> approveDraft(Course course) {
        CourseDraft draft = findDraft(course).orElse(null);
        if (draft == null) {
            return List.of();
        }
        List<Lesson> draftLessons = lessonRepository.findDraftByDraft(draft.getId());
        List<Lesson> publishedLessons = course.getStatus() == CourseStatus.PUBLISHED
                ? lessonRepository.findPublishedByCourse(course.getId())
                : List.of();
        List<Section> publishedSections = sectionRepository.findPublishedByCourse(course.getId());
        List<Section> draftSections = sectionRepository.findDraftByDraft(draft.getId());
        Map<String, Section> sectionsById = new HashMap<>();
        publishedSections.forEach(section -> sectionsById.put(section.getId(), section));
        draftSections.forEach(section -> sectionsById.put(section.getId(), section));
        List<LessonChangedEvent> lessonChangedEvents = lessonChangedEvents(course, draft, draftLessons, publishedLessons, sectionsById);
        courseMapper.copyDraftToCourse(draft, course);
        lessonRepository.deleteAll(publishedLessons);
        sectionRepository.deleteAll(publishedSections);
        draftSections.forEach(section -> {
            section.setCourseId(course.getId());
            section.setDraftId(null);
        });
        sectionRepository.saveAll(draftSections);
        courseDraftRepository.delete(draft);
        return lessonChangedEvents;
    }

    @Override
    public CourseDraft createDraftFromCourse(Course course) {
        CourseDraft draft = courseMapper.toDraft(course);
        return courseDraftRepository.save(draft);
    }

    private CourseDraft clonePublishedToDraft(Course course) {
        CourseDraft draft = createDraftFromCourse(course);
        Map<String, Section> clonedSections = new HashMap<>();
        List<Section> publishedSections = sectionRepository.findPublishedByCourse(course.getId());
        List<Section> sectionClones = new ArrayList<>();
        for (Section section : publishedSections) {
            Section clone = sectionMapper.cloneSection(section);
            clone.setDraftId(draft.getId());
            sectionClones.add(clone);
        }
        List<Section> savedSectionClones = sectionRepository.saveAll(sectionClones);
        for (int i = 0; i < publishedSections.size(); i++) {
            clonedSections.put(publishedSections.get(i).getId(), savedSectionClones.get(i));
        }

        List<Lesson> lessonClones = new ArrayList<>();
        for (Lesson lesson : lessonRepository.findPublishedByCourse(course.getId())) {
            Lesson clone = lessonMapper.cloneLesson(lesson);
            clone.setSectionId(clonedSections.get(lesson.getSectionId()).getId());
            clone.setPrerequisiteIds(new ArrayList<>(lesson.getPrerequisiteIds() == null ? List.of() : lesson.getPrerequisiteIds()));
            lessonClones.add(clone);
        }
        lessonRepository.saveAll(lessonClones);
        return draft;
    }

    private List<LessonChangedEvent> lessonChangedEvents(
            Course course,
            CourseDraft draft,
            List<Lesson> draftLessons,
            List<Lesson> publishedLessons,
            Map<String, Section> sectionsById
    ) {
        Map<String, Lesson> draftByStableId = draftLessons.stream()
                .collect(HashMap::new, (map, lesson) -> map.put(lesson.getStableId(), lesson), HashMap::putAll);
        Map<String, Lesson> publishedByStableId = publishedLessons.stream()
                .collect(HashMap::new, (map, lesson) -> map.put(lesson.getStableId(), lesson), HashMap::putAll);
        String title = draft.getTitle() != null ? draft.getTitle() : course.getTitle();
        List<LessonChangedEvent> events = new ArrayList<>();

        draftLessons.forEach(lesson -> {
            Lesson published = publishedByStableId.get(lesson.getStableId());
            if (published == null) {
                events.add(new LessonChangedEvent(course.getId(), lesson.getStableId(), "CREATED", title));
            } else if (lessonChanged(published, lesson, sectionsById)) {
                events.add(new LessonChangedEvent(course.getId(), lesson.getStableId(), "UPDATED", title));
            }
        });

        publishedLessons.stream()
                .filter(lesson -> !draftByStableId.containsKey(lesson.getStableId()))
                .forEach(lesson -> events.add(new LessonChangedEvent(course.getId(), lesson.getStableId(), "DELETED", title)));

        return events;
    }

    private boolean lessonChanged(Lesson published, Lesson draft, Map<String, Section> sectionsById) {
        return !Objects.equals(sectionsById.get(published.getSectionId()).getStableId(), sectionsById.get(draft.getSectionId()).getStableId())
                || !Objects.equals(published.getTitle(), draft.getTitle())
                || !Objects.equals(published.getDuration(), draft.getDuration())
                || !Objects.equals(published.getOrderIndex(), draft.getOrderIndex())
                || !Objects.equals(published.getLessonType(), draft.getLessonType())
                || !Objects.equals(published.getIsPreview(), draft.getIsPreview())
                || !Objects.equals(
                        published.getPrerequisiteIds() == null ? List.of() : published.getPrerequisiteIds(),
                        draft.getPrerequisiteIds() == null ? List.of() : draft.getPrerequisiteIds()
                );
    }
}

