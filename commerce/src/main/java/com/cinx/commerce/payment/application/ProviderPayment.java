package com.cinx.commerce.payment.application;

import java.math.BigDecimal;

/**
 * What a verified webhook says about one attempt.
 *
 * @param providerEventId the gateway's own id for the event or transaction; deduplicated per provider
 * @param paid true only for a settled payment, false for a definite failure
 */
public record ProviderPayment(String providerReference, String providerEventId, BigDecimal amount, boolean paid) {}
