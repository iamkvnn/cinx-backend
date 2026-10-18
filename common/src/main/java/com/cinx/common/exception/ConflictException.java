package com.cinx.common.exception;

public class ConflictException extends RuntimeException implements ErrorCodedException {
    private final ErrorCode errorCode;

    public ConflictException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
