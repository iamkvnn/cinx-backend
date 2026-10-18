package com.cinx.common.exception;

public class AlreadyExistException extends RuntimeException implements ErrorCodedException {
    private final ErrorCode errorCode;

    public AlreadyExistException(String message) {
        this(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }

    public AlreadyExistException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
