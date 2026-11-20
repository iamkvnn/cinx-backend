package com.cinx.commerce.catalog.application.query;

import com.cinx.commerce.catalog.domain.SellableCourse;

import java.math.BigDecimal;

/** A course as commerce may sell it: what the cart, the vouchers and the order lines need to know. */
public record SellableCourseView(String courseId, String title, BigDecimal price, String categoryId, String instructorId,
                                 boolean sellable) {

    public static SellableCourseView of(SellableCourse course) {
        return new SellableCourseView(course.getCourseId(), course.getTitle(), course.getPrice(), course.getCategoryId(),
                course.getInstructorId(), course.isSellable());
    }
}
