package com.cinx.learning.enrollment.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.enrollment.application.query.CheckEnrollmentStatus;
import com.cinx.learning.enrollment.application.query.CourseEntitlementInfo;
import com.cinx.learning.enrollment.domain.CourseAccessGrantRepository;
import com.cinx.learning.enrollment.domain.CourseEnrollmentRepository;
import com.cinx.learning.enrollment.domain.CourseEntitlement;
import com.cinx.learning.enrollment.domain.CourseEntitlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentAccessServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock
    private CourseEntitlementRepository entitlementRepository;
    @Mock
    private CourseAccessGrantRepository accessGrantRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentAccessService service;

    @Test
    void anActiveGrantIsAskedForWithTheActiveStatusAtTheGivenInstant() {
        when(accessGrantRepository.existsActive("user-1", "course-1", "ACTIVE", NOW)).thenReturn(true);

        assertThat(service.hasActiveGrant("user-1", "course-1", NOW)).isTrue();
    }

    @Test
    void anEnrollmentCountsOnlyWhileItIsActive() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus("user-1", "course-1", "ACTIVE")).thenReturn(true);

        assertThat(service.hasActiveEnrollment("user-1", "course-1")).isTrue();
        verify(enrollmentRepository).existsByUserIdAndCourseIdAndStatus("user-1", "course-1", "ACTIVE");
    }

    @Test
    void theEntitledCoursesAreOnlyTheOnesTheLearnerOwns() {
        when(entitlementRepository.findAllByUserIdAndCourseIdIn("user-1", List.of("a", "b", "c")))
                .thenReturn(List.of(entitlement("a"), entitlement("c")));

        assertThat(service.entitledCourseIds("user-1", List.of("a", "b", "c"))).isEqualTo(Set.of("a", "c"));
    }

    @Test
    void theStatusAnswersForEveryAskedCourseInTheAskedOrder() {
        when(entitlementRepository.findAllByUserIdAndCourseIdIn("user-1", List.of("a", "b")))
                .thenReturn(List.of(entitlement("b")));

        ApiResponse<List<CheckEnrollmentStatus>> response = service.checkEnrollmentStatus("user-1", List.of("a", "b"));

        assertThat(response.success()).isTrue();
        assertThat(response.data()).containsExactly(new CheckEnrollmentStatus("a", false), new CheckEnrollmentStatus("b", true));
    }

    @Test
    void theCourseCountsAndGrantsAreHandedOverAsPlainRows() {
        when(entitlementRepository.countPerCourse(List.of("a")))
                .thenReturn(List.<CourseEntitlementRepository.CourseCount>of(new CourseEntitlementRepository.CourseCount() {
                    @Override
                    public String getCourseId() {
                        return "a";
                    }

                    @Override
                    public long getTotal() {
                        return 4;
                    }
                }));
        when(entitlementRepository.findGrantsBetween(List.of("a"), NOW, NOW.plusDays(1)))
                .thenReturn(List.<CourseEntitlementRepository.GrantRow>of(new CourseEntitlementRepository.GrantRow() {
                    @Override
                    public String getCourseId() {
                        return "a";
                    }

                    @Override
                    public LocalDateTime getGrantedAt() {
                        return NOW;
                    }
                }));

        assertThat(service.countPerCourse(List.of("a"))).containsExactly(new EnrollmentAccess.CourseCount("a", 4));
        assertThat(service.grantsBetween(List.of("a"), NOW, NOW.plusDays(1)))
                .containsExactly(new EnrollmentAccess.GrantRow("a", NOW));
    }

    @Test
    void theEntitlementsOfACourseAreCopiedSoTheDomainStaysPrivate() {
        when(entitlementRepository.findAllByCourseId("a")).thenReturn(List.of(entitlement("a")));

        List<CourseEntitlementInfo> rows = service.entitlementsOfCourse("a");

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getCourseId()).isEqualTo("a");
            assertThat(row.getUserId()).isEqualTo("user-1");
        });
    }

    private static CourseEntitlement entitlement(String courseId) {
        return CourseEntitlement.builder().id("e-" + courseId).userId("user-1").courseId(courseId).grantedAt(NOW).build();
    }
}
