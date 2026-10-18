package com.cinx.common.exception;

public record FieldValidationError(String field, String message, Object rejectedValue) {
}
