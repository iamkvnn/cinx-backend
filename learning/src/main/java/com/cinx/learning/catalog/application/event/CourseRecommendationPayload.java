package com.cinx.learning.catalog.application.event;

import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.application.query.CategoryView;
import com.cinx.learning.catalog.application.query.CourseImageView;
import com.cinx.learning.catalog.application.query.CurriculumSectionView;
import com.cinx.learning.catalog.application.query.InstructorView;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record CourseRecommendationPayload(
        String id,
        String title,
        String description,
        CategoryView category,
        InstructorView instructor,
        List<CourseImageView> images,
        BigDecimal price,
        BigDecimal discountedPrice,
        Long discountRate,
        Double rating,
        Long enrollmentCount,
        Boolean isInSubscription,
        String accessModel,
        String subscriptionFeatureKey,
        Long duration,
        Boolean hasCertificate,
        String certificateTitle,
        CourseStatus status,
        CoursePublishStatus publishStatus,
        List<CurriculumSectionView> sections,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

