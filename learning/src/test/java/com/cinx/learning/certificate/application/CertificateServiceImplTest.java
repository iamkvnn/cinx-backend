package com.cinx.learning.certificate.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.certificate.application.mapper.CertificateRequestMapper;
import com.cinx.learning.certificate.application.query.CertificateRequestView;
import com.cinx.learning.certificate.domain.CertificateRequestRepository;
import com.cinx.learning.certificate.domain.CourseCertificateConditionsRepository;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.instructor.application.IInstructorActivityService;
import com.cinx.learning.instructor.application.InstructorCourseLookup;
import com.cinx.learning.media.application.FileStorage;
import com.cinx.learning.notificationprojection.application.LearningNotifications;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import com.cinx.learning.shared.application.UserDirectory;
import com.cinx.learning.shared.domain.CertificateStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceImplTest {
    @Mock
    private CertificateRequestRepository certificateRequestRepository;
    @Mock
    private CertificateRequestMapper certificateRequestMapper;
    @Mock
    private CourseService courseService;
    @Mock
    private InstructorCourseLookup courseLookup;
    @Mock
    private UserDirectory userService;
    @Mock
    private CertificateGeneratorService certificateGeneratorService;
    @Mock
    private FileStorage s3Service;
    @Mock
    private LearningAuthorizationService authorizationService;
    @Mock
    private LearningNotifications notificationPublisher;
    @Mock
    private CertificateEligibilityService eligibilityService;
    @Mock
    private CourseCertificateConditionsRepository conditionsRepository;
    @Mock
    private IInstructorActivityService activityService;

    @InjectMocks
    private CertificateServiceImpl certificateService;

    @Test
    void getRequestsByCoursePassesQueryStatusAndSortToRepository() {
        ArgumentCaptor<PageQuery> pageCaptor = ArgumentCaptor.forClass(PageQuery.class);
        when(certificateRequestRepository.search(eq("course-1"), eq(CertificateStatus.PENDING), eq("user-1"), pageCaptor.capture()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));

        certificateService.getRequestsByCourse(
                "instructor-1",
                "course-1",
                CertificateStatus.PENDING,
                1,
                10,
                " user-1 ",
                "{\"requestedAt\":\"DESC\"}");

        // the search text is trimmed and the sort travels as the client wrote it
        assertThat(pageCaptor.getValue().sort()).isEqualTo("{\"requestedAt\":\"DESC\"}");
        assertThat(pageCaptor.getValue().page()).isEqualTo(1);
        assertThat(pageCaptor.getValue().size()).isEqualTo(10);
        verify(authorizationService).requireCourseInstructor("instructor-1", "course-1");
    }

    @Test
    void getAllRequestsLimitsResultsToCurrentInstructorCourses() {
        ArgumentCaptor<PageQuery> pageCaptor = ArgumentCaptor.forClass(PageQuery.class);
        List<String> instructorCourseIds = List.of("course-1", "course-2");
        when(courseLookup.managedCourses("instructor-1")).thenReturn(instructorCourseIds.stream()
                .map(id -> new InstructorCourseLookup.CourseCard(id, id, null, null, null, null))
                .toList());
        when(certificateRequestRepository.searchByCourseIds(
                eq(instructorCourseIds),
                eq(CertificateStatus.PENDING),
                eq("student-1"),
                pageCaptor.capture()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));

        certificateService.getAllRequests(
                "instructor-1",
                CertificateStatus.PENDING,
                1,
                10,
                " student-1 ",
                "{\"requestedAt\":\"DESC\"}");

        assertThat(pageCaptor.getValue().sort()).isEqualTo("{\"requestedAt\":\"DESC\"}");
        verify(certificateRequestRepository, never()).search(eq(null), any(), any(), any());
    }

    @Test
    void getAllRequestsReturnsEmptyPageWhenInstructorHasNoCourses() {
        when(courseLookup.managedCourses("instructor-1")).thenReturn(List.of());

        PageResult<CertificateRequestView> result = certificateService.getAllRequests(
                "instructor-1",
                null,
                1,
                10,
                null,
                null);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(certificateRequestRepository, never()).searchByCourseIds(any(), any(), any(), any());
        verify(certificateRequestRepository, never()).search(eq(null), any(), any(), any());
    }
}
