package com.cinx.learning.catalog.application.query;

import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record CourseView (
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
        Long ratingCount,
        Long enrollmentCount,
        Boolean isInSubscription,
        Long duration,
        Boolean hasCertificate,
        String certificateTitle,
        CourseStatus status,
        CoursePublishStatus publishStatus,
        List<String> outcomes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt,
        LocalDateTime submittedAt,
        LocalDateTime archivedAt,
        String promoVideoUrl,
        Integer promoVideoDuration,
        Boolean aiAssisted
) {
    /** The public view: the instructor without an email. */
    public CourseView withoutInstructorEmail() {
        return instructor == null ? this : new CourseView(id, title, description, category, instructor.withoutEmail(), images,
                price, discountedPrice, discountRate, rating, ratingCount, enrollmentCount, isInSubscription, duration,
                hasCertificate, certificateTitle, status, publishStatus, outcomes, createdAt, updatedAt,
                publishedAt, submittedAt, archivedAt, promoVideoUrl, promoVideoDuration, aiAssisted);
    }
}

