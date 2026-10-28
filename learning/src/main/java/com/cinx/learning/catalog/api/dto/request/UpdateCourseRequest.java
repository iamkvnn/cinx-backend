package com.cinx.learning.catalog.api.dto.request;

import com.cinx.common.validation.FieldLessThan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@FieldLessThan(message = "Discounted price must be less than original price")
public record UpdateCourseRequest(
        @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
        @Schema(example = "Advanced Spring Boot Microservices")
        String title,
        
        @Schema(example = "Learn advanced techniques for microservices")
        String description,
        
        @Schema(example = "cat_123")
        @NotBlank(message = "Category ID is required")
        String categoryId,
        
        // cinx sells no free courses (operator, 2026-09-29): both prices must be above zero
        @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "Price must have at most 17 integer and 2 decimal digits")
        @NotNull(message = "Price is required")
        @Schema(example = "1200000")
        BigDecimal price,

        @DecimalMin(value = "0.00", inclusive = false, message = "Discounted price must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "Discounted price must have at most 17 integer and 2 decimal digits")
        @Schema(example = "1000000")
        BigDecimal discountedPrice,
        
        @Schema(example = "true")
        Boolean isInSubscription,
        
        @Min(value = 0, message = "Duration cannot be negative")
        @Schema(example = "150")
        Long duration,
        
        @Schema(example = "true")
        Boolean hasCertificate,
        
        @Schema(example = "Spring Boot Expert Certificate")
        String certificateTitle,

        @Size(max = 5, message = "At most 5 outcomes")
        @Schema(example = "[\"Build a 5-service order system on Docker\"]")
        List<@NotBlank(message = "Outcome must not be blank") @Size(max = 120, message = "Each outcome is at most 120 characters") String> outcomes,

        @Schema(description = "true when an AI draft helped write the description; turns the course label on, never off", example = "false")
        Boolean aiAssisted
) {
}

