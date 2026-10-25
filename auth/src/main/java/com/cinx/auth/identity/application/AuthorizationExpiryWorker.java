package com.cinx.auth.identity.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationExpiryWorker {
    private final IdentityService identityService;

    @Scheduled(fixedDelayString = "${identity.authorization-expiry.fixed-delay-ms:5000}")
    void expire() {
        identityService.expireDueGrants();
    }
}
