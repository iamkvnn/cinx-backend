package com.cinx.learning.catalog.application.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.CourseDraft;

/** What other features may read of a CourseDraft; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class CourseDraftInfo {
    String id;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
    String updatedBy;
    String courseId;
    String title;
    String description;
    Long duration;
    BigDecimal price;
    BigDecimal discountedPrice;
    Long discountRate;
    Boolean isInSubscription;
    Boolean hasCertificate;
    String certificateTitle;
    String categoryId;
    List<String> outcomes;

    public static CourseDraftInfo of(CourseDraft domain) {
        return CourseDraftInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .courseId(domain.getCourseId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .duration(domain.getDuration())
                .price(domain.getPrice())
                .discountedPrice(domain.getDiscountedPrice())
                .discountRate(domain.getDiscountRate())
                .isInSubscription(domain.getIsInSubscription())
                .hasCertificate(domain.getHasCertificate())
                .certificateTitle(domain.getCertificateTitle())
                .categoryId(domain.getCategoryId())
                .outcomes(domain.getOutcomes())
                .build();
    }
}
