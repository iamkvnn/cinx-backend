package com.cinx.learning.shared.application;

import com.cinx.learning.catalog.application.ICourseAccessService;
import com.cinx.learning.courseview.application.CourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Who may take part in a course's Q&A: the same people LessonService lets into a lesson. */
@ExtendWith(MockitoExtension.class)
class LearningAuthorizationServiceTest {
    @Mock
    private CourseService courseService;
    @Mock
    private ICourseAccessService courseAccessService;

    @InjectMocks
    private LearningAuthorizationService service;

    @Test
    void noUserOrNoCourseNeverParticipates() {
        assertThat(service.canParticipateInCourse(null, "course-1")).isFalse();
        assertThat(service.canParticipateInCourse("user-1", null)).isFalse();
        verifyNoInteractions(courseAccessService);
    }

    @Test
    void outsiderDoesNotParticipate() {
        assertThat(service.canParticipateInCourse("outsider", "course-1")).isFalse();
    }

    @Test
    void adminParticipates() {
        when(courseAccessService.isAdmin()).thenReturn(true);

        assertThat(service.canParticipateInCourse("admin-1", "course-1")).isTrue();
    }

    @Test
    void anyActiveTeamMemberParticipates() {
        when(courseAccessService.isActiveMember("editor-1", "course-1")).thenReturn(true);

        assertThat(service.canParticipateInCourse("editor-1", "course-1")).isTrue();
    }

    @Test
    void enrolledLearnerParticipates() {
        when(courseAccessService.isEnrolled("learner-1", "course-1")).thenReturn(true);

        assertThat(service.canParticipateInCourse("learner-1", "course-1")).isTrue();
    }

    @Test
    void subscriptionLearnerParticipatesWithoutAnEnrollment() {
        when(courseAccessService.hasSubscriptionAccess("subscriber-1", "course-1")).thenReturn(true);

        assertThat(service.canParticipateInCourse("subscriber-1", "course-1")).isTrue();
    }
}
