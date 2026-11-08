package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.domain.FeatureDefinition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureRegistryValidatorTest {
    @Test
    void acceptsSaleableBooleanAndQuotaDefinitions() {
        FeatureRegistryValidator.validate(List.of(
                new FeatureDefinition("learning.subscription-catalog.access", "learning",
                        FeatureDefinition.ValueType.BOOLEAN, null, Set.of(), true, true,
                        Set.of("learning.lesson-content-read")),
                new FeatureDefinition("recommendation.agent.requests", "recommendation",
                        FeatureDefinition.ValueType.QUOTA, "REQUEST", Set.of("CALENDAR_MONTH"),
                        true, true, Set.of("recommendation.agent-use"))));
    }

    @Test
    void acceptsCreditQuotaAndRejectsUnknownUnit() {
        FeatureRegistryValidator.validate(List.of(
                new FeatureDefinition("recommendation.agent.requests", "recommendation",
                        FeatureDefinition.ValueType.QUOTA, "CREDIT", Set.of("CALENDAR_MONTH"),
                        true, true, Set.of("recommendation.agent-use"))));
        assertThatThrownBy(() -> FeatureRegistryValidator.validate(List.of(
                new FeatureDefinition("recommendation.agent.requests", "recommendation",
                        FeatureDefinition.ValueType.QUOTA, "TOKEN", Set.of("CALENDAR_MONTH"),
                        true, true, Set.of("recommendation.agent-use")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown quota unit");
    }

    @Test
    void rejectsSaleableFeatureWithoutOperationBinding() {
        assertThatThrownBy(() -> FeatureRegistryValidator.validate(List.of(
                new FeatureDefinition("learning.offline-download", "learning",
                        FeatureDefinition.ValueType.BOOLEAN, null, Set.of(), false, true, Set.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saleable feature");
    }

    @Test
    void rejectsQuotaWithoutResetPolicyAndOwnerMismatch() {
        assertThatThrownBy(() -> FeatureRegistryValidator.validate(List.of(
                new FeatureDefinition("recommendation.agent.requests", "recommendation",
                        FeatureDefinition.ValueType.QUOTA, "REQUEST", Set.of(), true, false,
                        Set.of("learning.lesson-content-read")))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
