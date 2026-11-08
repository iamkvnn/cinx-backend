package com.cinx.commerce.payment.api.dto;

import com.cinx.commerce.payment.domain.PaymentProviderName;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(@NotNull PaymentProviderName provider) {
}
