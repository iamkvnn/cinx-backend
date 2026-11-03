package com.cinx.commerce.wishlist.api.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record WishlistResponse(String courseId, String title, BigDecimal price, LocalDateTime addedAt) {
}
