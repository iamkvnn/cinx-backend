package com.cinx.commerce.payment.api.dto;

import com.cinx.commerce.payment.domain.PaymentProviderName;
import java.time.LocalDateTime;

/** Where to send the learner to pay; the link stops working at {@code expiresAt}. */
public record PaymentLinkResponse(String paymentUrl, PaymentProviderName provider, LocalDateTime expiresAt) {
}
