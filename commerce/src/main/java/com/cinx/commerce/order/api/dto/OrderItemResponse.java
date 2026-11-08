package com.cinx.commerce.order.api.dto;

import java.math.BigDecimal;

public record OrderItemResponse(String courseId, String title, BigDecimal price) {
}
