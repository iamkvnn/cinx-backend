package com.cinx.learning.catalog.domain;

import com.cinx.learning.shared.domain.CoursePublishStatus;

import com.cinx.learning.shared.domain.CourseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Course extends AuditedModel {
    private String title;
    private String description;
    private Long duration;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Long discountRate;
    private Double rating;
    @Builder.Default
    private Long ratingCount = 0L;
    private Long enrollmentCount;
    private Boolean isInSubscription;
    private CourseAccessModel accessModel;
    private String subscriptionFeatureKey;
    private String instructorId;
    @Builder.Default
    private Boolean hasCertificate = false;
    private String certificateTitle;
    /** An AI draft helped write the description or outline; only ever turned on. */
    @Builder.Default
    private Boolean aiAssisted = false;
    private CourseStatus status;
    private CoursePublishStatus publishStatus;
    /** First approval; never cleared, so a course that was ever on sale cannot be hard-deleted. */
    private LocalDateTime publishedAt;
    /** The pending review request; cleared on approve, reject and withdraw. */
    private LocalDateTime submittedAt;
    private LocalDateTime archivedAt;
    /** Intro video; lives on the course like the images, so a change is live at once. Duration in seconds. */
    private String promoVideoKey;
    private String promoVideoUrl;
    private String promoVideoFileName;
    private Long promoVideoFileSize;
    private Integer promoVideoDuration;
    private String categoryId;
    @Builder.Default
    private List<CourseImage> images = new ArrayList<>();
    @Builder.Default
    private List<String> outcomes = new ArrayList<>();

    /** The first image that has an address: the card shows it as the cover. */
    public String thumbnailUrl() {
        return images == null ? null : images.stream().map(CourseImage::getImageUrl).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }
}
