package com.cinx.commerce.order.application;

import java.util.Set;

/** What the cart and the vouchers may ask the orders about a learner's past purchases. */
public interface PurchaseHistory {
    /** The courses the learner already owns as far as commerce knows: those in their paid orders. */
    Set<String> paidCourseIds(String userId);

    boolean hasPaidOrder(String userId);
}
