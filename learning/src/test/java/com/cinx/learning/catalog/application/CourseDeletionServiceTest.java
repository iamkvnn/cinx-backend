package com.cinx.learning.catalog.application;

import com.cinx.common.exception.ConflictException;
import com.cinx.learning.catalog.domain.ArticleLessonRepository;
import com.cinx.learning.catalog.domain.AssignmentAttachmentRepository;
import com.cinx.learning.catalog.domain.AssignmentLessonRepository;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseAccessPolicyRepository;
import com.cinx.learning.catalog.domain.CourseDraftRepository;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.CourseReviewNoteRepository;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.QuizLessonRepository;
import com.cinx.learning.catalog.domain.QuizOptionRepository;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import com.cinx.learning.catalog.domain.RejectCourseReasonRepository;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.SectionRepository;
import com.cinx.learning.catalog.domain.SubtitleJobRepository;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.catalog.domain.VideoQuestionRepository;
import com.cinx.learning.enrollment.application.EnrollmentAccess;
import com.cinx.learning.shared.application.CourseDeletionHook;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CourseDeletionServiceTest {
    private EnrollmentAccess enrollmentAccess;
    private CourseRepository courseRepository;
    private CourseDraftRepository courseDraftRepository;
    private SectionRepository sectionRepository;
    private VideoLessonRepository videoLessonRepository;
    private ICourseAccessService courseAccessService;
    private CourseDeletionHook firstHook;
    private CourseDeletionHook secondHook;
    private CourseDeletionService service;

    @BeforeEach
    void setUp() {
        enrollmentAccess = mock(EnrollmentAccess.class);
        courseRepository = mock(CourseRepository.class);
        courseDraftRepository = mock(CourseDraftRepository.class);
        sectionRepository = mock(SectionRepository.class);
        videoLessonRepository = mock(VideoLessonRepository.class);
        courseAccessService = mock(ICourseAccessService.class);
        firstHook = mock(CourseDeletionHook.class);
        secondHook = mock(CourseDeletionHook.class);
        service = new CourseDeletionService(enrollmentAccess, courseRepository, courseDraftRepository, sectionRepository,
                videoLessonRepository, mock(VideoQuestionRepository.class), mock(SubtitleTrackRepository.class),
                mock(SubtitleJobRepository.class), mock(ArticleLessonRepository.class), mock(AssignmentLessonRepository.class),
                mock(AssignmentAttachmentRepository.class), mock(QuizLessonRepository.class), mock(QuizQuestionRepository.class),
                mock(QuizOptionRepository.class), mock(RejectCourseReasonRepository.class), mock(CourseReviewNoteRepository.class),
                mock(CourseAccessPolicyRepository.class), mock(CourseInstructorMembershipRepository.class), courseAccessService,
                mock(ObjectStorage.class), List.of(firstHook, secondHook));
    }

    @Test
    void everyFeatureRemovesItsDataBeforeTheLessonContentGoes() {
        Course course = draftCourse();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(sectionRepository.findPublishedByCourse("course-1")).thenReturn(List.of(section("lesson-a", "lesson-b")));
        when(courseDraftRepository.findByCourse("course-1")).thenReturn(Optional.empty());
        when(videoLessonRepository.findByLessonId(any())).thenReturn(Optional.empty());

        service.deleteDraftCourse("owner-1", "course-1");

        InOrder order = inOrder(firstHook, secondHook, videoLessonRepository, courseRepository);
        order.verify(firstHook).onCourseDeleted("course-1", new java.util.LinkedHashSet<>(List.of("lesson-a", "lesson-b")));
        order.verify(secondHook).onCourseDeleted("course-1", new java.util.LinkedHashSet<>(List.of("lesson-a", "lesson-b")));
        order.verify(videoLessonRepository).findByLessonId("lesson-a");
        order.verify(courseRepository).delete(course);
    }

    @Test
    void aCourseWithLearnersIsNotDeletedAndNoFeatureIsAskedToRemoveAnything() {
        Course course = draftCourse();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(enrollmentAccess.courseHasEntitlements("course-1")).thenReturn(true);

        assertThatThrownBy(() -> service.deleteDraftCourse("owner-1", "course-1"))
                .isInstanceOfSatisfying(ConflictException.class, e -> org.assertj.core.api.Assertions.assertThat(e.getMessage()).isEqualTo("HAS_LEARNERS"));

        verifyNoInteractions(firstHook, secondHook);
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void aCourseThatWasEverPublishedIsNotDeletable() {
        Course course = draftCourse();
        course.setPublishedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.deleteDraftCourse("owner-1", "course-1"))
                .isInstanceOfSatisfying(ConflictException.class, e -> org.assertj.core.api.Assertions.assertThat(e.getMessage()).isEqualTo("WAS_PUBLISHED"));

        verifyNoInteractions(firstHook, secondHook);
    }

    @Test
    void aCourseInReviewIsNotDeletable() {
        Course course = draftCourse();
        course.setPublishStatus(CoursePublishStatus.WAITING_APPROVAL);
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.deleteDraftCourse("owner-1", "course-1"))
                .isInstanceOfSatisfying(ConflictException.class, e -> org.assertj.core.api.Assertions.assertThat(e.getMessage()).isEqualTo("IN_REVIEW"));

        verifyNoInteractions(firstHook, secondHook);
    }

    private static Course draftCourse() {
        return Course.builder().id("course-1").status(CourseStatus.DRAFT).images(new ArrayList<>()).build();
    }

    private static Section section(String... lessonStableIds) {
        Section section = new Section();
        section.setId("section-1");
        List<Lesson> lessons = new ArrayList<>();
        for (String stableId : lessonStableIds) {
            lessons.add(Lesson.builder().stableId(stableId).build());
        }
        section.setLessons(lessons);
        return section;
    }
}
