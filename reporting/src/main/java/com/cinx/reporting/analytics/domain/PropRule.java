package com.cinx.reporting.analytics.domain;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What one event prop may hold. Strings are codes and ids only, never free text, so nothing personal can be smuggled in.
 */
public record PropRule(PropType type, boolean required, int maxLength, long min, long max, List<String> values) {
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_.:-]+");
    private static final Pattern KEY = Pattern.compile("[a-z0-9_]{1,64}");

    public static PropRule id(boolean required) {
        return new PropRule(PropType.ID, required, 64, 0, 0, List.of());
    }

    public static PropRule key(boolean required) {
        return new PropRule(PropType.KEY, required, 64, 0, 0, List.of());
    }

    public static PropRule string(int maxLength) {
        return new PropRule(PropType.STRING, false, maxLength, 0, 0, List.of());
    }

    public static PropRule integer(long min, long max, boolean required) {
        return new PropRule(PropType.INTEGER, required, 0, min, max, List.of());
    }

    public static PropRule bool() {
        return new PropRule(PropType.BOOLEAN, false, 0, 0, 0, List.of());
    }

    public static PropRule oneOf(boolean required, String... values) {
        return new PropRule(PropType.ENUM, required, 0, 0, 0, List.of(values));
    }

    /** The value as stored (integers become longs), or empty when it does not fit this rule. */
    public Optional<Object> accept(Object value) {
        return Optional.ofNullable(switch (type) {
            case ID, STRING -> value instanceof String s && s.length() <= maxLength && CODE.matcher(s).matches() ? s : null;
            case KEY -> value instanceof String s && KEY.matcher(s).matches() ? s : null;
            case ENUM -> value instanceof String s && values.contains(s) ? s : null;
            case BOOLEAN -> value instanceof Boolean b ? b : null;
            case INTEGER -> integral(value);
        });
    }

    private Long integral(Object value) {
        if (!(value instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())) {
            return null;
        }
        long n = number.longValue();
        return n >= min && n <= max ? n : null;
    }
}
