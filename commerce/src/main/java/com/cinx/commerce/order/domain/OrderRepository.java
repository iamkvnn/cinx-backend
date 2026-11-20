package com.cinx.commerce.order.domain;

import java.util.List;
import java.util.Optional;

/** Port: where orders, with their lines, are stored. */
public interface OrderRepository {
    Optional<CommerceOrder> findById(String id);

    Optional<CommerceOrder> findByIdAndUserId(String id, String userId);

    Optional<CommerceOrder> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    /** Newest first; undated (older pending) orders come last. */
    List<CommerceOrder> findAllByUserId(String userId);

    /** The order with a row lock held until the surrounding transaction ends. */
    Optional<CommerceOrder> lockById(String id);

    /** The courses the user already owns as far as commerce knows: those in their paid orders. */
    List<String> findPaidCourseIds(String userId);

    boolean existsByUserIdAndStatus(String userId, OrderStatus status);

    /** Stores the order. A new order is written out at once so rows that point at it can follow in the same transaction. */
    CommerceOrder save(CommerceOrder order);
}
