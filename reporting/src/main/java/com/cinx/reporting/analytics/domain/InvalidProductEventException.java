package com.cinx.reporting.analytics.domain;

/** An event or experiment that breaks the dictionary or the experiment rules. */
public class InvalidProductEventException extends RuntimeException {
    public InvalidProductEventException(String message) {
        super(message);
    }
}
