package com.cinx.learning.enrollment.application;

import com.cinx.learning.enrollment.domain.CourseEntitlement;
import com.cinx.learning.enrollment.domain.CourseAccessGrant;
import com.cinx.learning.enrollment.domain.CourseEnrollment;
import com.cinx.learning.enrollment.domain.CourseAccessGrantRepository;
import com.cinx.learning.enrollment.domain.CourseEnrollmentRepository;
import com.cinx.learning.enrollment.domain.CourseEntitlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;

@Service
public class EntitlementService {
    private final CourseEntitlementRepository repository;
    private final EnrollmentEvents eventPublisher;
    private final CourseAccessGrantRepository accessGrantRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Autowired
    public EntitlementService(CourseEntitlementRepository repository,
                               EnrollmentEvents eventPublisher,
                               CourseAccessGrantRepository accessGrantRepository,
                               CourseEnrollmentRepository enrollmentRepository,
                               Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.accessGrantRepository = accessGrantRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.clock = clock;
    }

    /** Compatibility seam for existing unit tests and legacy callers. */
    public EntitlementService(CourseEntitlementRepository repository,
                               EnrollmentEvents eventPublisher) {
        this(repository, eventPublisher, null, null, Clock.systemUTC());
    }

    @Transactional
    public boolean grant(String userId, String courseId) {
        return grant(null, userId, courseId);
    }

    @Transactional
    public boolean grant(String orderId, String userId, String courseId) {
        String id = orderId == null ? userId + ":" + courseId : orderId + ":" + courseId;
        if (repository.existsById(id)) {
            return false;
        }
        if (repository.existsByUserIdAndCourseId(userId, courseId)) {
            // A legacy ADMIN entitlement may predate a paid order.  Keep the
            // legacy row for compatibility, but still record the immutable
            // PURCHASE grant keyed by the provider order.
            if (orderId != null) {
                ensureAccessGrant(userId, courseId, "PURCHASE", orderId, LocalDateTime.now(clock));
            }
            return false;
        }
        CourseEntitlement entitlement;
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            entitlement = repository.saveAndFlush(CourseEntitlement.builder()
                    .id(id)
                    .userId(userId)
                    .courseId(courseId)
                    .sourceOrderId(orderId)
                    .grantedAt(now)
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
        String source = orderId == null ? "ADMIN" : "PURCHASE";
        String sourceReference = orderId == null ? entitlement.getId() : orderId;
        ensureAccessGrant(userId, courseId, source, sourceReference, now);
        if (enrollmentRepository != null
                && !enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, "ACTIVE")) {
            enrollmentRepository.save(CourseEnrollment.builder()
                    .id("enrollment:" + userId + ":" + courseId)
                    .userId(userId)
                    .courseId(courseId)
                    .enrolledAt(now)
                    .accessSourceAtEnrollment(source)
                    .status("ACTIVE")
                    .build());
        }
        eventPublisher.publishGranted(entitlement);
        return true;
    }

    private void ensureAccessGrant(String userId, String courseId, String source,
                                   String sourceReference, LocalDateTime now) {
        if (accessGrantRepository == null
                || accessGrantRepository.existsByUserIdAndCourseIdAndSourceAndSourceReference(
                userId, courseId, source, sourceReference)) {
            return;
        }
        accessGrantRepository.save(CourseAccessGrant.builder()
                .id("grant:" + source.toLowerCase() + ":" + sourceReference + ":" + courseId)
                .userId(userId)
                .courseId(courseId)
                .source(source)
                .sourceReference(sourceReference)
                .validFrom(now)
                .status("ACTIVE")
                .build());
    }
}
