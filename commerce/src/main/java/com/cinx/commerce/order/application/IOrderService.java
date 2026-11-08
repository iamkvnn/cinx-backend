package com.cinx.commerce.order.application;

import com.cinx.commerce.order.application.command.CheckoutCommand;
import com.cinx.commerce.order.application.query.OrderView;

import java.util.List;

public interface IOrderService {
    OrderView checkout(String userId, String idempotencyKey, CheckoutCommand command);

    List<OrderView> findAll(String userId);

    OrderView findById(String userId, String orderId);

    OrderView findByIdAny(String orderId);

    /** Cancels the caller's own PENDING order; the voucher use stays consumed. */
    OrderView cancel(String userId, String orderId);
}
