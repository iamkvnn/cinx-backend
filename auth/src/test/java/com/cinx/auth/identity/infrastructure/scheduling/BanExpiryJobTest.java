package com.cinx.auth.identity.infrastructure.scheduling;

import com.cinx.auth.identity.application.IIdentityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BanExpiryJobTest {
    @Mock IIdentityService identities;
    @InjectMocks BanExpiryJob job;

    @Test
    void oneFailedLiftDoesNotStopTheOthers() {
        when(identities.findExpiredBans(BanExpiryJob.BATCH)).thenReturn(List.of("a", "b", "c"));
        doThrow(new IllegalStateException("keycloak down")).when(identities).liftExpiredBan("b");

        job.liftExpiredBans();

        verify(identities).liftExpiredBan("a");
        verify(identities).liftExpiredBan("b");
        verify(identities).liftExpiredBan("c");
    }
}
