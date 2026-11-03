package com.cinx.commerce.cart.application;

import java.util.List;

/** What checkout and the voucher preview may ask the cart. */
public interface CartContents {
    /** The courses in the learner's cart, newest first. */
    List<String> courseIdsOf(String userId);

    void clear(String userId);
}
