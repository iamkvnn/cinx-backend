package com.cinx.commerce.cart.domain;

import java.util.List;

/** Port: where a learner's cart is stored. */
public interface CartRepository {
    /** Newest first. */
    List<CartItem> findAllByUserId(String userId);

    boolean existsByUserIdAndCourseId(String userId, String courseId);

    CartItem save(CartItem item);

    /** Whether an item was removed. */
    boolean deleteByUserIdAndCourseId(String userId, String courseId);

    void deleteAllByUserId(String userId);
}
