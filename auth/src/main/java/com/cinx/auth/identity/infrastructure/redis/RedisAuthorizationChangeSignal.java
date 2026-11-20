package com.cinx.auth.identity.infrastructure.redis;

import com.cinx.auth.identity.application.event.AuthorizationChanged;
import com.cinx.common.security.AuthorizationChangeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Tells the gateway, through Redis, that tokens issued before a committed change are stale. A failure is logged, not
 * thrown: the change is already stored, and its tokens still expire with the realm's access-token lifespan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RedisAuthorizationChangeSignal {
    /** Keeps the newer change when two commits for one user write out of order; the time is the value's last word. */
    static final RedisScript<Long> SET_IF_NEWER = RedisScript.of("""
            local current = redis.call('GET', KEYS[1])
            if current then
              local at = tonumber(string.match(current, ' (%d+)$'))
              if at and at >= tonumber(ARGV[2]) then return 0 end
            end
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onCommitted(AuthorizationChanged change) {
        try {
            redis.execute(SET_IF_NEWER, List.of(AuthorizationChangeKey.key(change.userId())),
                    AuthorizationChangeKey.encode(change.status().name(), change.changedAt()),
                    String.valueOf(change.changedAt().toEpochMilli()),
                    String.valueOf(AuthorizationChangeKey.TTL.toSeconds()));
        } catch (RuntimeException unavailable) {
            log.warn("Authorization change not signalled to Redis; tokens stay usable until they expire userId={} reason={}",
                    change.userId(), unavailable.toString());
        }
    }
}
