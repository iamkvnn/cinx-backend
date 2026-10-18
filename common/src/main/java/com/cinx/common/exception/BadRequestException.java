package com.cinx.common.exception;

import java.util.Map;

public class BadRequestException extends RuntimeException implements ErrorCodedException {
    private final ErrorCode errorCode;
    /** Extra problem-detail properties, e.g. {@code errors} listing what a COURSE_NOT_READY submit is missing. */
    private final Map<String, Object> properties;

    public BadRequestException(String message) {
        this(ErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public BadRequestException(ErrorCode errorCode, String message, Map<String, Object> properties) {
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
