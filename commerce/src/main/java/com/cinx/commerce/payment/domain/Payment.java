package com.cinx.commerce.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private String id;
    private String orderId;
    private String provider;
    private String providerEventId;
    private BigDecimal amount;
    private LocalDateTime paidAt;
}
