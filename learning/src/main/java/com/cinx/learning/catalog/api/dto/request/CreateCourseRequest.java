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

@FieldLessThan(message = "Discounted price must be less than original price")
public record CreateCourseRequest (
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
        @Schema(example = "Spring Boot Microservices")
        String title,
        
        @NotBlank(message = "Description is required")
        @Schema(example = "Learn how to build microservices with Spring Boot 3")
        String description,
        
        @NotBlank(message = "Category ID is required")
        @Schema(example = "cat_123")
        String categoryId,
        
        @NotNull(message = "Price is required")
        // cinx sells no free courses (operator, 2026-09-29): both prices must be above zero
        @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "Price must have at most 17 integer and 2 decimal digits")
        @Schema(example = "1000000.00")
        BigDecimal price,

        @DecimalMin(value = "0.00", inclusive = false, message = "Discounted price must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "Discounted price must have at most 17 integer and 2 decimal digits")
        @Schema(example = "800000.00")
        BigDecimal discountedPrice,
        
        @NotNull(message = "Subscription inclusion flag is required")
        @Schema(example = "false")
        Boolean isInSubscription,
        
        @Min(value = 0, message = "Duration cannot be negative")
        Long duration,
        
        @NotNull(message = "Certificate flag is required")
        Boolean hasCertificate,
        
        String certificateTitle
) {
}

