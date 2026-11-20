package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.domain.FeatureDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Semantic checks that cannot be expressed by the registry JSON schema. */
public final class FeatureRegistryValidator {
    private static final Pattern KEY = Pattern.compile("^[a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)+$");
    private static final Set<String> OWNERS = Set.of("learning", "recommendation", "notification", "reporting", "assistant");
    private static final Set<String> UNITS = Set.of("REQUEST", "MINUTE", "CREDIT");
    private static final Set<String> RESET_POLICIES = Set.of("CALENDAR_MONTH", "SUBSCRIPTION_PERIOD");

    private FeatureRegistryValidator() {
    }

    public static void validate(List<FeatureDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        var keys = new HashSet<String>();
        for (FeatureDefinition definition : definitions) {
            require(definition != null, "feature entry is required");
            require(definition.key() != null && KEY.matcher(definition.key()).matches(),
                    "invalid feature key: " + definition.key());
            require(!definition.key().startsWith("auth."), "authentication feature namespace is forbidden");
            require(!isAuthorizationValue(definition.key()), "authorization role/permission/scope cannot be a feature key");
            require(keys.add(definition.key()), "duplicate feature key: " + definition.key());
            require(definition.owner() != null && !definition.owner().isBlank(), "feature owner is required");
            require(OWNERS.contains(definition.owner()), "unknown feature owner: " + definition.owner());
            require(definition.valueType() != null, "feature value type is required: " + definition.key());
            var resets = definition.resetPolicies() == null ? java.util.Set.<String>of() : definition.resetPolicies();
            var bindings = definition.operationBindings() == null ? java.util.Set.<String>of() : definition.operationBindings();
            if (definition.valueType() == FeatureDefinition.ValueType.BOOLEAN) {
                require(definition.unit() == null && resets.isEmpty(),
                        "boolean feature cannot define quota metadata: " + definition.key());
            } else {
                require(definition.unit() != null && !resets.isEmpty(),
                        "quota feature requires unit and reset policy: " + definition.key());
                require(UNITS.contains(definition.unit()), "unknown quota unit: " + definition.unit());
            }
            resets.forEach(reset -> require(RESET_POLICIES.contains(reset), "unknown reset policy: " + reset));
            if (definition.saleable()) {
                require(!bindings.isEmpty(), "saleable feature must bind an operation: " + definition.key());
            }
            for (String operation : bindings) {
                require(operation != null && operation.contains("."), "invalid operation binding: " + operation);
                require(!isAuthorizationValue(operation), "authorization role/permission/scope cannot be an operation binding");
                var operationOwner = operation.substring(0, operation.indexOf('.'));
                require(definition.owner().equals(operationOwner),
                        "feature owner does not own operation " + operation);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean isAuthorizationValue(String value) {
        return value != null && (value.startsWith("ROLE_") || value.startsWith("PERM_") || value.startsWith("SCOPE_"));
    }
}
