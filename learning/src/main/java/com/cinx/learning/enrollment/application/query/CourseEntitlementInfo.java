package com.cinx.learning.enrollment.application.query;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.enrollment.domain.CourseEntitlement;

/** What other features may read of a CourseEntitlement; a copy, so the domain class stays private to enrollment. */
@Value
@Builder
public class CourseEntitlementInfo {
    String id;
    String userId;
    String courseId;
    String sourceOrderId;
    LocalDateTime grantedAt;

    public static CourseEntitlementInfo of(CourseEntitlement domain) {
        return CourseEntitlementInfo.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .courseId(domain.getCourseId())
                .sourceOrderId(domain.getSourceOrderId())
                .grantedAt(domain.getGrantedAt())
                .build();
    }
}
