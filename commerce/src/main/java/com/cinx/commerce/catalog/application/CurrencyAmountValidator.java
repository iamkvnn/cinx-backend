package com.cinx.commerce.catalog.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Converts decimal catalog amounts to provider minor units without binary rounding. */
public final class CurrencyAmountValidator {
    private static final Map<String, Integer> EXPONENTS = Map.of(
            "VND", 0,
            "JPY", 0,
            "KRW", 0,
            "USD", 2,
            "EUR", 2);

    private CurrencyAmountValidator() {}

    public static long toMinorAmount(BigDecimal amount, String currency) {
        if (amount == null || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Amount and currency are required");
        }
        int exponent = EXPONENTS.getOrDefault(currency.toUpperCase(java.util.Locale.ROOT), -1);
        if (exponent < 0) throw new IllegalArgumentException("Unsupported currency exponent: " + currency);
        try {
            return amount.movePointRight(exponent).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount has fractional minor units for " + currency, exception);
        }
    }
}
