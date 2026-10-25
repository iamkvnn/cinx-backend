package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.CourseAccessModel;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.Course;

/** What other features may read of a Course; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class CourseInfo {
    String id;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
    String updatedBy;
    String title;
    String description;
    Long duration;
    BigDecimal price;
    BigDecimal discountedPrice;
    Long discountRate;
    Double rating;
    Long ratingCount;
    Long enrollmentCount;
    Boolean isInSubscription;
    CourseAccessModel accessModel;
    String subscriptionFeatureKey;
    String instructorId;
    Boolean hasCertificate;
    String certificateTitle;
    CourseStatus status;
    CoursePublishStatus publishStatus;
    LocalDateTime publishedAt;
    LocalDateTime submittedAt;
    LocalDateTime archivedAt;
    String promoVideoKey;
    String promoVideoUrl;
    String promoVideoFileName;
    Long promoVideoFileSize;
    Integer promoVideoDuration;
    String categoryId;
    List<CourseImageInfo> images;
    List<String> outcomes;

    public static CourseInfo of(Course domain) {
        return CourseInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .duration(domain.getDuration())
                .price(domain.getPrice())
                .discountedPrice(domain.getDiscountedPrice())
                .discountRate(domain.getDiscountRate())
                .rating(domain.getRating())
                .ratingCount(domain.getRatingCount())
                .enrollmentCount(domain.getEnrollmentCount())
                .isInSubscription(domain.getIsInSubscription())
                .accessModel(domain.getAccessModel())
                .subscriptionFeatureKey(domain.getSubscriptionFeatureKey())
                .instructorId(domain.getInstructorId())
                .hasCertificate(domain.getHasCertificate())
                .certificateTitle(domain.getCertificateTitle())
                .status(domain.getStatus())
                .publishStatus(domain.getPublishStatus())
                .publishedAt(domain.getPublishedAt())
                .submittedAt(domain.getSubmittedAt())
                .archivedAt(domain.getArchivedAt())
                .promoVideoKey(domain.getPromoVideoKey())
                .promoVideoUrl(domain.getPromoVideoUrl())
                .promoVideoFileName(domain.getPromoVideoFileName())
                .promoVideoFileSize(domain.getPromoVideoFileSize())
                .promoVideoDuration(domain.getPromoVideoDuration())
                .categoryId(domain.getCategoryId())
                .images(domain.getImages() == null ? null : domain.getImages().stream().map(CourseImageInfo::of).toList())
                .outcomes(domain.getOutcomes())
                .build();
    }

    /** The first image that has an address: the card shows it as the cover. */
    public String thumbnailUrl() {
        return images == null ? null : images.stream().map(CourseImageInfo::getImageUrl).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }
}
