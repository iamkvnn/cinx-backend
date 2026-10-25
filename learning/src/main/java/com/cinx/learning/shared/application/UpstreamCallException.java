package com.cinx.learning.shared.application;

/** A call to another service failed; {@link #isNotFound()} tells an unknown resource from an outage. */
public class UpstreamCallException extends RuntimeException {
    private final boolean notFound;

    public UpstreamCallException(boolean notFound, Throwable cause) {
        super(cause);
        this.notFound = notFound;
    }

    public boolean isNotFound() {
        return notFound;
    }
}
