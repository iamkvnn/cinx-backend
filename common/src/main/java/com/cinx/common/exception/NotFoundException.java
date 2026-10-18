package com.cinx.common.exception;

public class NotFoundException extends RuntimeException implements ErrorCodedException {
    private final ErrorCode errorCode;

    public NotFoundException(String message) {
        this(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public NotFoundException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
