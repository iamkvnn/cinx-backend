package com.cinx.commerce.cart.application;

import com.cinx.commerce.cart.application.query.CartItemView;

import java.util.List;

public interface ICartService {
    CartItemView add(String userId, String courseId);

    List<CartItemView> findAll(String userId);

    void remove(String userId, String courseId);
}
