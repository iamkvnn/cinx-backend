package com.cinx.learning.catalog.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.command.CreateSectionCommand;
import com.cinx.learning.catalog.application.command.MoveSectionCommand;
import com.cinx.learning.catalog.application.command.UpdateSectionCommand;
import com.cinx.learning.catalog.application.query.SectionPositionView;
import com.cinx.learning.catalog.application.query.SectionView;
import com.cinx.learning.catalog.application.mapper.SectionMapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionService implements ISectionService {
    private final CourseRepository courseRepository;
    private final ICourseDraftService courseDraftService;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final SectionMapper sectionMapper;
    private final ICourseAccessService courseAccessService;

    @Transactional
    @Override
    public SectionView createSection(String currentUserId, String courseId, CreateSectionCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        CourseDraft draft = courseDraftService.getOrCreateDraft(course);
        Section section = sectionMapper.toModel(request);
        section.setStableId(UUID.randomUUID().toString());
        section.setDraftId(draft.getId());
        section.setOrderIndex(nextSectionOrderIndex(draft.getId()));
        if (Boolean.TRUE.equals(request.aiAssisted())) {
            courseDraftService.markAiAssisted(course);
        }
        return sectionMapper.toResponse(sectionRepository.save(section));
    }

    @Transactional
    @Override
    public SectionView updateSection(String currentUserId, String courseId, String sectionId, UpdateSectionCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        Section section = editableSection(courseId, sectionId);
        sectionMapper.partialUpdate(section, request);
        return sectionMapper.toResponse(sectionRepository.save(section));
    }

    @Transactional
    @Override
    public SectionPositionView moveSection(String currentUserId, String courseId, String sectionId, MoveSectionCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        CourseDraft draft = courseDraftService.getOrCreateDraft(course);
        List<Section> currentSections = sectionRepository.findDraftByDraftForUpdate(draft.getId());
        Map<String, Section> sectionsByStableId = currentSections.stream()
                .collect(Collectors.toMap(Section::getStableId, Function.identity()));
        Section movedSection = sectionsByStableId.get(sectionId);
        if (movedSection == null) {
            throw new NotFoundException("Section not found with id: " + sectionId);
        }

        List<Section> remainingSections = currentSections.stream()
                .filter(section -> !Objects.equals(section.getStableId(), sectionId))
                .collect(Collectors.toCollection(ArrayList::new));
        int targetIndex = OrderIndexUtils.insertionIndex(
                remainingSections,
                request.previousSectionId(),
                request.nextSectionId(),
                Section::getStableId,
                sectionId,
                "section"
        );
        Integer newOrderIndex = OrderIndexUtils.midpointOrderIndex(
                remainingSections,
                targetIndex,
                Section::getOrderIndex
        );
        if (newOrderIndex != null) {
            if (!Objects.equals(movedSection.getOrderIndex(), newOrderIndex)) {
                movedSection.setOrderIndex(newOrderIndex);
                sectionRepository.save(movedSection);
            }
            return new SectionPositionView(movedSection.getStableId(), movedSection.getOrderIndex());
        }

        List<Section> desiredSections = new ArrayList<>(remainingSections);
        desiredSections.add(targetIndex, movedSection);
        List<Section> changedSections = OrderIndexUtils.rebalance(
                desiredSections,
                Section::getOrderIndex,
                Section::setOrderIndex
        );
        if (!changedSections.isEmpty()) {
            sectionRepository.saveAll(changedSections);
        }
        return new SectionPositionView(movedSection.getStableId(), movedSection.getOrderIndex());
    }

    @Transactional
    @Override
    public void deleteSection(String currentUserId, String courseId, String sectionId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        Section section = editableSection(courseId, sectionId);
        List<Lesson> lessons = lessonRepository.findBySectionIdsForUpdate(List.of(section.getId()));
        lessonRepository.deleteAll(lessons);
        section.getLessons().clear();
        sectionRepository.delete(section);
    }

    @Override
    public Section editableSection(String courseId, String sectionId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        CourseDraft editableDraft = courseDraftService.getOrCreateDraft(course);
        return sectionRepository.findDraftSection(editableDraft.getId(), sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found with id: " + sectionId));
    }

    private Integer nextSectionOrderIndex(String draftId) {
        return sectionRepository.findDraftByDraftForUpdate(draftId).stream()
                .map(Section::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + OrderIndexUtils.ORDER_STEP;
    }
}

