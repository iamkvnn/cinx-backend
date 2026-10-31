package com.cinx.learning.enrollment.application;

import com.cinx.learning.enrollment.domain.CourseAccessGrant;
import com.cinx.learning.enrollment.domain.CourseAccessGrantRepository;
import com.cinx.learning.enrollment.domain.CourseEnrollment;
import com.cinx.learning.enrollment.domain.CourseEnrollmentRepository;
import com.cinx.learning.enrollment.domain.CourseEntitlement;
import com.cinx.learning.enrollment.domain.CourseEntitlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock
    CourseEntitlementRepository repository;
    @Mock
    EnrollmentEvents eventPublisher;
    @Mock
    CourseAccessGrantRepository accessGrantRepository;
    @Mock
    CourseEnrollmentRepository enrollmentRepository;

    private EntitlementService service() {
        return new EntitlementService(repository, eventPublisher, accessGrantRepository, enrollmentRepository, CLOCK);
    }

    @Test
    void grantFlushesBeforePublishingEnrollmentEvent() {
        when(repository.saveAndFlush(any(CourseEntitlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean granted = service().grant("order-1", "user-1", "course-1");

        assertThat(granted).isTrue();
        ArgumentCaptor<CourseEntitlement> captor = ArgumentCaptor.forClass(CourseEntitlement.class);
        verify(eventPublisher).publishGranted(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("order-1:course-1");
        assertThat(captor.getValue().getGrantedAt()).isEqualTo(NOW);
    }

    @Test
    void duplicateUniqueConstraintDoesNotPublishEnrollmentEvent() {
        when(repository.saveAndFlush(any(CourseEntitlement.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate entitlement"));

        boolean granted = service().grant("order-1", "user-1", "course-1");

        assertThat(granted).isFalse();
        verify(eventPublisher, never()).publishGranted(any());
    }

    @Test
    void aPaidOrderRecordsAPurchaseGrantAndAnActiveEnrollment() {
        when(repository.saveAndFlush(any(CourseEntitlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().grant("order-1", "user-1", "course-1");

        ArgumentCaptor<CourseAccessGrant> grant = ArgumentCaptor.forClass(CourseAccessGrant.class);
        verify(accessGrantRepository).save(grant.capture());
        assertThat(grant.getValue().getId()).isEqualTo("grant:purchase:order-1:course-1");
        assertThat(grant.getValue().getSource()).isEqualTo("PURCHASE");
        assertThat(grant.getValue().getSourceReference()).isEqualTo("order-1");
        assertThat(grant.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(grant.getValue().getValidFrom()).isEqualTo(NOW);
        ArgumentCaptor<CourseEnrollment> enrollment = ArgumentCaptor.forClass(CourseEnrollment.class);
        verify(enrollmentRepository).save(enrollment.capture());
        assertThat(enrollment.getValue().getId()).isEqualTo("enrollment:user-1:course-1");
        assertThat(enrollment.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(enrollment.getValue().getAccessSourceAtEnrollment()).isEqualTo("PURCHASE");
    }

    @Test
    void anAdminGrantWithoutAnOrderUsesTheEntitlementAsItsReference() {
        when(repository.saveAndFlush(any(CourseEntitlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().grant("user-1", "course-1");

        ArgumentCaptor<CourseAccessGrant> grant = ArgumentCaptor.forClass(CourseAccessGrant.class);
        verify(accessGrantRepository).save(grant.capture());
        assertThat(grant.getValue().getSource()).isEqualTo("ADMIN");
        assertThat(grant.getValue().getSourceReference()).isEqualTo("user-1:course-1");
    }

    @Test
    void aReplayedOrderChangesNothing() {
        when(repository.existsById("order-1:course-1")).thenReturn(true);

        boolean granted = service().grant("order-1", "user-1", "course-1");

        assertThat(granted).isFalse();
        verify(repository, never()).saveAndFlush(any());
        verify(accessGrantRepository, never()).save(any());
        verify(enrollmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishGranted(any());
    }

    @Test
    void aLegacyAdminEntitlementStillGetsThePurchaseGrantOfALaterOrder() {
        when(repository.existsByUserIdAndCourseId("user-1", "course-1")).thenReturn(true);

        boolean granted = service().grant("order-2", "user-1", "course-1");

        assertThat(granted).isFalse();
        ArgumentCaptor<CourseAccessGrant> grant = ArgumentCaptor.forClass(CourseAccessGrant.class);
        verify(accessGrantRepository).save(grant.capture());
        assertThat(grant.getValue().getId()).isEqualTo("grant:purchase:order-2:course-1");
        verify(repository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishGranted(any());
    }

    @Test
    void anExistingActiveEnrollmentIsNotWrittenTwice() {
        when(repository.saveAndFlush(any(CourseEntitlement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus("user-1", "course-1", "ACTIVE")).thenReturn(true);

        service().grant("order-1", "user-1", "course-1");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void aGrantThatIsAlreadyRecordedIsNotRecordedAgain() {
        when(repository.saveAndFlush(any(CourseEntitlement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accessGrantRepository.existsByUserIdAndCourseIdAndSourceAndSourceReference("user-1", "course-1", "PURCHASE", "order-1"))
                .thenReturn(true);

        service().grant("order-1", "user-1", "course-1");

        verify(accessGrantRepository, never()).save(any());
    }
}
