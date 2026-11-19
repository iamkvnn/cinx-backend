package com.cinx.auth.identity.infrastructure.scheduling;

import com.cinx.auth.identity.application.IIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ends timed bans. Each user is lifted in its own transaction, so one Keycloak failure leaves the rest to go on and
 * that user to the next run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BanExpiryJob {
    static final int BATCH = 100;

    private final IIdentityService identities;

    @Scheduled(fixedDelayString = "${app.ban-expiry.delay-ms:300000}")
    void liftExpiredBans() {
        for (String userId : identities.findExpiredBans(BATCH)) {
            try {
                identities.liftExpiredBan(userId);
            } catch (RuntimeException failure) {
                log.warn("Expired ban not lifted; retried on the next run userId={} reason={}", userId, failure.toString());
            }
        }
    }
}
