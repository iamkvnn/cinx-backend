package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAccessPolicy {
    private String courseId;
    private CourseAccessModel accessModel;
    private String subscriptionFeatureKey;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private String changedBy;
    private String reason;
    private long version;
}
