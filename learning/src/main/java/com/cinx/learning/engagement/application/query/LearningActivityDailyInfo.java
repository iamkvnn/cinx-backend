package com.cinx.learning.engagement.application.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.engagement.domain.LearningActivityDaily;

/** What other features may read of a LearningActivityDaily; a copy, so the domain class stays private to engagement. */
@Value
@Builder
public class LearningActivityDailyInfo {
    String id;
    Long version;
    String userId;
    String courseId;
    LocalDate activityDate;
    Long activeSeconds;
    LocalDateTime lastActivityAt;
    String lastItemId;

    public static LearningActivityDailyInfo of(LearningActivityDaily domain) {
        return LearningActivityDailyInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .userId(domain.getUserId())
                .courseId(domain.getCourseId())
                .activityDate(domain.getActivityDate())
                .activeSeconds(domain.getActiveSeconds())
                .lastActivityAt(domain.getLastActivityAt())
                .lastItemId(domain.getLastItemId())
                .build();
    }
}
