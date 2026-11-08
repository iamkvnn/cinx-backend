package com.cinx.commerce.catalog.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyAmountValidatorTest {
    @Test
    void mapsVndToIntegerMinorUnits() {
        assertThat(CurrencyAmountValidator.toMinorAmount(new BigDecimal("99000"), "VND"))
                .isEqualTo(99000L);
    }

    @Test
    void rejectsFractionalVnd() {
        assertThatThrownBy(() -> CurrencyAmountValidator.toMinorAmount(new BigDecimal("99000.50"), "VND"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
