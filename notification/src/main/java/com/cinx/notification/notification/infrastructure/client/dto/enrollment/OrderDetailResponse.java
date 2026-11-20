package com.cinx.notification.notification.infrastructure.client.dto.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record OrderDetailResponse(
        @Schema(example = "ord_123")
        String id,
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "150000")
        BigDecimal totalPrice,
        @Schema(example = "50000")
        BigDecimal discounted
) {}
