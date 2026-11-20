package com.cinx.learning.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.support.RetryTemplate;

class ConsumerConfigRetryTest {

    private final RetryTemplate retry = RetryTemplate.builder()
            .customPolicy(ConsumerConfig.transientDatabaseRetryPolicy())
            .noBackoff()
            .build();

    @Test
    void aDeadlockWrappedByTheContainerIsRetriedUntilItSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        String result = retry.execute(context -> {
            if (attempts.incrementAndGet() < 3) {
                throw new ListenerExecutionFailedException("listener failed",
                        new CannotAcquireLockException("Deadlock found when trying to get lock"));
            }
            return "applied";
        });
        assertThat(result).isEqualTo("applied");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void aDeadlockStopsAfterThreeAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> retry.execute(context -> {
            attempts.incrementAndGet();
            throw new ListenerExecutionFailedException("listener failed", new CannotAcquireLockException("deadlock"));
        })).isInstanceOf(ListenerExecutionFailedException.class);
        assertThat(attempts).hasValue(3);
    }

    @Test
    void anyOtherFailureIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> retry.execute(context -> {
            attempts.incrementAndGet();
            throw new ListenerExecutionFailedException("listener failed", new IllegalStateException("bad payload"));
        })).isInstanceOf(ListenerExecutionFailedException.class);
        assertThat(attempts).hasValue(1);
    }
}
