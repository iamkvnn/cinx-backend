package com.cinx.commerce.catalog.domain;

import java.util.Set;

/** Immutable runtime representation of one entry in the feature registry. */
public record FeatureDefinition(
        String key,
        String owner,
        ValueType valueType,
        String unit,
        Set<String> resetPolicies,
        boolean publicFeature,
        boolean saleable,
        Set<String> operationBindings) {

    public enum ValueType { BOOLEAN, QUOTA }
}
