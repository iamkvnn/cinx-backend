package com.cinx.learning.community.application.query;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.community.domain.Review;

/** What other features may read of a Review; a copy, so the domain class stays private to community. */
@Value
@Builder
public class ReviewInfo {
    String id;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
    String updatedBy;
    String userId;
    String courseId;
    Double rating;
    String content;

    public static ReviewInfo of(Review domain) {
        return ReviewInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .userId(domain.getUserId())
                .courseId(domain.getCourseId())
                .rating(domain.getRating())
                .content(domain.getContent())
                .build();
    }
}
