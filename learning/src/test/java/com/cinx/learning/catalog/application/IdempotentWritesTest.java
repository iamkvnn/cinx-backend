package com.cinx.learning.catalog.application;

import com.cinx.common.exception.ConflictException;
import com.cinx.learning.catalog.application.query.SectionView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotentWritesTest {
    /** In-memory port: the uniqueness of (user, key) is what the database enforces. */
    private static final class MemoryRecords implements IdempotencyRecords {
        final Map<String, Stored> rows = new HashMap<>();

        @Override
        public Optional<Stored> find(String userId, String key) {
            return Optional.ofNullable(rows.get(userId + "|" + key));
        }

        @Override
        public void save(String userId, String key, Stored record) {
            rows.put(userId + "|" + key, record);
        }
    }

    private final MemoryRecords records = new MemoryRecords();
    private final IdempotentWrites writes = new IdempotentWrites(records, new ObjectMapper());
    private final AtomicInteger creates = new AtomicInteger();

    private SectionView create(String userId, String key, Object request) {
        return writes.once(userId, key, "section.create:c1", request, SectionView.class,
                () -> new SectionView("s-" + creates.incrementAndGet(), "Intro", null, 0L, 1), SectionView::id);
    }

    @Test
    void theSameKeyAndBodyCreateOnceAndReturnTheFirstSection() {
        SectionView first = create("u1", "k1", Map.of("title", "Intro"));
        SectionView retry = create("u1", "k1", Map.of("title", "Intro"));

        assertThat(creates.get()).isEqualTo(1);
        assertThat(retry).isEqualTo(first);
    }

    @Test
    void theSameKeyWithAnotherBodyIsAConflict() {
        create("u1", "k1", Map.of("title", "Intro"));

        assertThatThrownBy(() -> create("u1", "k1", Map.of("title", "Other")))
                .isInstanceOf(ConflictException.class);
        assertThat(creates.get()).isEqualTo(1);
    }

    @Test
    void keysAreScopedToTheUserAndNoKeyAlwaysCreates() {
        create("u1", "k1", Map.of("title", "Intro"));
        create("u2", "k1", Map.of("title", "Intro"));
        create("u1", null, Map.of("title", "Intro"));
        create("u1", " ", Map.of("title", "Intro"));

        assertThat(creates.get()).isEqualTo(4);
    }
}
