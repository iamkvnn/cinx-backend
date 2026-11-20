package com.cinx.commerce.wishlist.domain;

import java.util.List;

/** Port: where a learner's wishlist is stored. */
public interface WishlistRepository {
    boolean existsByUserIdAndCourseId(String userId, String courseId);

    WishlistItem save(WishlistItem item);

    /** Newest first. */
    List<WishlistItem> findAllByUserId(String userId);

    /** Whether an item was removed. */
    boolean deleteByUserIdAndCourseId(String userId, String courseId);
}
