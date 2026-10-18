package com.cinx.common.security;

import org.springframework.security.access.AccessDeniedException;

public class MfaStepUpRequiredException extends AccessDeniedException {
    public MfaStepUpRequiredException() {
        super("MFA step-up required");
    }
}
