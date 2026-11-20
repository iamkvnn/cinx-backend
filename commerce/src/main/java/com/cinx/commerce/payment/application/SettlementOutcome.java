package com.cinx.commerce.payment.application;

/** What settling a verified provider notice did; the webhook layer maps it to each gateway's reply. */
public enum SettlementOutcome {
    /** The order was just marked paid. */
    APPLIED,
    /** The same provider event, or an order that is already paid: nothing changed. */
    DUPLICATE,
    /** The gateway reported a definite failure for this attempt. */
    NOT_PAID,
    /** No attempt or order matches the gateway's reference. */
    NOT_FOUND,
    /** The paid amount differs from the attempt or the order total. */
    AMOUNT_MISMATCH,
    /** The money arrived after the order was cancelled; nothing is granted. */
    ORDER_CANCELLED
}
