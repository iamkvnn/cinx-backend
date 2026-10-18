package com.cinx.common.exception;

import java.util.Map;

public class ForbiddenException extends RuntimeException implements ErrorCodedException {
    private final ErrorCode errorCode;
    /** Extra problem-detail properties, e.g. {@code missingPrerequisiteIds} for LESSON_LOCKED. */
    private final Map<String, Object> properties;

    public ForbiddenException(String message) {
        this(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public ForbiddenException(ErrorCode errorCode, String message, Map<String, Object> properties) {
        super(message);
        this.errorCode = errorCode;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
