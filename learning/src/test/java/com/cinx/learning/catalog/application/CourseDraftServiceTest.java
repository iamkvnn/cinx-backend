package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.UpdateCourseCommand;
import com.cinx.learning.catalog.application.mapper.CourseMapperImpl;
import com.cinx.learning.catalog.application.mapper.LessonMapper;
import com.cinx.learning.catalog.application.mapper.SectionMapper;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.CourseDraftRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SectionRepository;
import com.cinx.learning.shared.domain.CourseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The course AI label: client-declared, only ever turned on, carried from the draft to the course on approval. */
@ExtendWith(MockitoExtension.class)
class CourseDraftServiceTest {
    @Mock
    private CourseDraftRepository courseDraftRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private SectionMapper sectionMapper;
    @Mock
    private LessonMapper lessonMapper;

    private CourseDraftService service;

    @BeforeEach
    void setUp() {
        service = new CourseDraftService(courseDraftRepository, courseRepository, sectionRepository, lessonRepository,
                new CourseMapperImpl(), sectionMapper, lessonMapper);
    }

    @Test
    void approvingADraftCarriesItsAiLabelToTheCourse() {
        Course course = course(CourseStatus.PUBLISHED, false);
        when(courseDraftRepository.findByCourse("course-1")).thenReturn(Optional.of(draft(true)));

        service.approveDraft(course);

        assertThat(course.getAiAssisted()).isTrue();
    }

    @Test
    void anUpdateWithoutTheFlagNeverTurnsTheLabelOff() {
        CourseDraft draft = draft(true);
        when(courseDraftRepository.findByCourse("course-1")).thenReturn(Optional.of(draft));
        when(courseDraftRepository.save(any(CourseDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateDraft(course(CourseStatus.DRAFT, true), update(false), category(), null);
        service.updateDraft(course(CourseStatus.DRAFT, true), update(null), category(), null);

        assertThat(draft.getAiAssisted()).isTrue();
    }

    @Test
    void anUpdateDeclaredAiAssistedTurnsTheDraftLabelOn() {
        CourseDraft draft = draft(false);
        when(courseDraftRepository.findByCourse("course-1")).thenReturn(Optional.of(draft));
        when(courseDraftRepository.save(any(CourseDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateDraft(course(CourseStatus.DRAFT, false), update(true), category(), null);

        assertThat(draft.getAiAssisted()).isTrue();
    }

    @Test
    void anOutlineOnAnUnpublishedCourseLabelsTheDraftAndTheCourse() {
        Course course = course(CourseStatus.DRAFT, false);
        CourseDraft draft = draft(false);
        when(courseDraftRepository.findByCourse("course-1")).thenReturn(Optional.of(draft));

        service.markAiAssisted(course);

        assertThat(draft.getAiAssisted()).isTrue();
        assertThat(course.getAiAssisted()).isTrue();
        verify(courseRepository).save(course);
    }

    @Test
    void anOutlineOnAPublishedCourseWaitsForApproval() {
        Course course = course(CourseStatus.PUBLISHED, false);
        CourseDraft draft = draft(false);
        when(courseDraftRepository.findByCourse("course-1")).thenReturn(Optional.of(draft));

        service.markAiAssisted(course);

        assertThat(draft.getAiAssisted()).isTrue();
        assertThat(course.getAiAssisted()).isFalse();
        verify(courseRepository, never()).save(any());
    }

    private static Course course(CourseStatus status, boolean aiAssisted) {
        return Course.builder().id("course-1").title("Spring").status(status).aiAssisted(aiAssisted).build();
    }

    private static CourseDraft draft(boolean aiAssisted) {
        return CourseDraft.builder().id("draft-1").courseId("course-1").title("Spring").aiAssisted(aiAssisted).build();
    }

    private static Category category() {
        Category category = new Category();
        category.setId("cat-1");
        return category;
    }

    private static UpdateCourseCommand update(Boolean aiAssisted) {
        return new UpdateCourseCommand("Spring", "New description", "cat-1", new BigDecimal("499000"), null,
                null, null, null, null, List.of(), aiAssisted);
    }
}
