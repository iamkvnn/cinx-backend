package com.cinx.commerce.cart.api.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * A cart line. {@code sellable} is false when the course stopped selling or its projection is missing (title and price
 * are then null); {@code owned} is true when the course is in one of the caller's paid orders. Checkout refuses both.
 */
public record CartItemResponse(String id, String courseId, String title, BigDecimal price, LocalDateTime addedAt,
                               boolean sellable, boolean owned) {
}
