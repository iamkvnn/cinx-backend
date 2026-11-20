package com.cinx.commerce.order.application.command;

/** Checks the learner's cart out; a null or blank voucher code means no voucher. */
public record CheckoutCommand(String voucherCode) {
}
