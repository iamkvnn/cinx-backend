package com.cinx.commerce.wishlist.application.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A course on a learner's wishlist, with the title and price the catalog shows today. */
public record WishlistEntryView(String courseId, String title, BigDecimal price, LocalDateTime addedAt) {
}
