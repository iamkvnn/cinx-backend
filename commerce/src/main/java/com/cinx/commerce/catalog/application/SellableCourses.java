package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.query.SellableCourseView;

import java.util.Optional;

/** What the cart, the wishlist, the vouchers and the orders may ask the catalog about a course. */
public interface SellableCourses {
    Optional<SellableCourseView> findById(String courseId);

    /** The course when it exists and is on sale. */
    Optional<SellableCourseView> findSellable(String courseId);
}
