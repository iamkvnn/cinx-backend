package com.cinx.learning.enrollment.infrastructure.messaging;

import com.cinx.learning.enrollment.application.EnrollmentEvents;
import com.cinx.learning.shared.infrastructure.messaging.OutboxEventPublisher;

import com.cinx.learning.enrollment.domain.CourseEntitlement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EntitlementEventPublisher implements EnrollmentEvents {
    private final OutboxEventPublisher outbox;

    public void publishGranted(CourseEntitlement entitlement) {
        outbox.enqueue(
                entitlement.getId() + ":granted",
                "CourseEntitlement",
                entitlement.getId(),
                "LearningEnrollmentGranted",
                "learning.events.exchange",
                "learning.enrollment.granted",
                Map.of(
                        "userId", entitlement.getUserId(),
                        "courseId", entitlement.getCourseId(),
                        "grantedAt", entitlement.getGrantedAt().toString()
                )
        );
    }
}
