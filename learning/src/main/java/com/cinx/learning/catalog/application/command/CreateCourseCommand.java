package com.cinx.learning.catalog.application.command;

import com.cinx.common.validation.FieldLessThan;
import java.math.BigDecimal;

@FieldLessThan(message = "Discounted price must be less than original price")
public record CreateCourseCommand (
        String title,
        
        String description,
        
        String categoryId,
        
        // cinx sells no free courses (operator, 2026-09-29): both prices must be above zero
        BigDecimal price,

        BigDecimal discountedPrice,
        
        Boolean isInSubscription,
        
        Long duration,
        
        Boolean hasCertificate,
        
        String certificateTitle
) {
}

