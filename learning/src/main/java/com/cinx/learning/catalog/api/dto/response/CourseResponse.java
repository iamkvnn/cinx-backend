package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record CourseResponse (
        @Schema(example = "course_abc123")
        String id,
        @Schema(example = "Spring Boot Microservices")
        String title,
        @Schema(example = "Learn how to build microservices with Spring Boot 3")
        String description,
        CategoryResponse category,
        InstructorResponse instructor,
        List<CourseImageResponse> images,
        @Schema(example = "1000000")
        BigDecimal price,
        @Schema(example = "800000")
        BigDecimal discountedPrice,
        @Schema(example = "20")
        Long discountRate,
        @Schema(example = "4.8")
        Double rating,
        @Schema(example = "126")
        Long ratingCount,
        @Schema(example = "150")
        Long enrollmentCount,
        @Schema(example = "false")
        Boolean isInSubscription,
        @Schema(example = "120")
        Long duration,
        @Schema(example = "true")
        Boolean hasCertificate,
        @Schema(example = "Srping Boot Expert")
        String certificateTitle,
        @Schema(example = "PUBLISHED")
        CourseStatus status,
        @Schema(example = "WAITING_APPROVAL")
        CoursePublishStatus publishStatus,
        @Schema(example = "[\"Build a 5-service order system on Docker\"]")
        List<String> outcomes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt,
        LocalDateTime submittedAt,
        LocalDateTime archivedAt,
        @Schema(example = "https://cdn.example.com/courses/promo/intro.mp4")
        String promoVideoUrl,
        @Schema(example = "95", description = "Intro video length in seconds")
        Integer promoVideoDuration,
        @Schema(example = "false", description = "An AI draft helped write the description or outline")
        Boolean aiAssisted
) {
    /** The public view: the instructor without an email. */
    public CourseResponse withoutInstructorEmail() {
        return instructor == null ? this : new CourseResponse(id, title, description, category, instructor.withoutEmail(), images,
                price, discountedPrice, discountRate, rating, ratingCount, enrollmentCount, isInSubscription, duration,
                hasCertificate, certificateTitle, status, publishStatus, outcomes, createdAt, updatedAt,
                publishedAt, submittedAt, archivedAt, promoVideoUrl, promoVideoDuration, aiAssisted);
    }
}

