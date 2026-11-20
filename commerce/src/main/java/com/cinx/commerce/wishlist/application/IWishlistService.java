package com.cinx.commerce.wishlist.application;

import com.cinx.commerce.wishlist.application.query.WishlistEntryView;

import java.util.List;

public interface IWishlistService {
    WishlistEntryView add(String userId, String courseId);

    List<WishlistEntryView> findAll(String userId);

    void remove(String userId, String courseId);
}
