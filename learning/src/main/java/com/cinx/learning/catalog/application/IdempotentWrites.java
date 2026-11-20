package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs a catalog create at most once per (user, Idempotency-Key). The create's own transaction joins this one,
 * so the new resource and its record commit together; a retry with the same body gets the first resource back.
 */
@Component
@RequiredArgsConstructor
public class IdempotentWrites {
    private static final int MAX_KEY = 255;

    private final IdempotencyRecords records;
    private final ObjectMapper objectMapper;

    /**
     * @param type  the view's class, to read the first response back
     * @param idOf  the id of a newly created resource, kept for the audit
     */
    @Transactional
    public <T> T once(String userId, String key, String operation, Object request, Class<T> type,
                      Supplier<T> create, Function<T, String> idOf) {
        String normalized = normalize(key);
        if (normalized == null) {
            return create.get();
        }
        String hash = hash(operation, request);
        Optional<IdempotencyRecords.Stored> previous = records.find(userId, normalized);
        if (previous.isPresent()) {
            IdempotencyRecords.Stored stored = previous.get();
            if (!stored.operation().equals(operation) || !stored.requestHash().equals(hash)) {
                throw new ConflictException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency-Key was already used with another request");
            }
            return read(stored.responseJson(), type);
        }
        T created = create.get();
        records.save(userId, normalized, new IdempotencyRecords.Stored(operation, hash, idOf.apply(created), write(created)));
        return created;
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored idempotent response is unreadable", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot store the idempotent response", ex);
        }
    }

    private static String normalize(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.length() > MAX_KEY) {
            throw new BadRequestException("Idempotency-Key must be at most " + MAX_KEY + " characters");
        }
        return trimmed;
    }

    private String hash(String operation, Object request) {
        try {
            byte[] body = (operation + "\n" + objectMapper.writeValueAsString(request)).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot fingerprint the request", ex);
        }
    }
}
