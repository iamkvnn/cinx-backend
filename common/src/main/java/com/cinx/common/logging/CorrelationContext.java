package com.cinx.common.logging;

import org.slf4j.MDC;
import org.springframework.amqp.core.MessageProperties;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CorrelationContext {
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String USER_ID_KEY = "userId";
    public static final String METHOD_KEY = "method";
    public static final String PATH_KEY = "path";
    public static final String STATUS_KEY = "status";
    public static final String DURATION_MS_KEY = "durationMs";

    private static final Pattern TRACEPARENT_PATTERN =
            Pattern.compile("^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private CorrelationContext() {
    }

    public static TraceHeaders fromHeaders(String traceparent, String requestId) {
        String traceId;
        if (traceparent != null && TRACEPARENT_PATTERN.matcher(traceparent.trim().toLowerCase(Locale.ROOT)).matches()) {
            traceId = traceparent.trim().substring(3, 35).toLowerCase(Locale.ROOT);
        } else {
            traceId = randomHex(16);
        }
        String spanId = randomHex(8);
        String normalizedRequestId = requestId == null || requestId.isBlank() ? traceId : requestId.trim();
        return new TraceHeaders(traceId, spanId, normalizedRequestId);
    }

    public static void put(TraceHeaders headers) {
        MDC.put(TRACE_ID_KEY, headers.traceId());
        MDC.put(SPAN_ID_KEY, headers.spanId());
        MDC.put(REQUEST_ID_KEY, headers.requestId());
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove(REQUEST_ID_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(METHOD_KEY);
        MDC.remove(PATH_KEY);
        MDC.remove(STATUS_KEY);
        MDC.remove(DURATION_MS_KEY);
    }

    public static String currentTraceparent() {
        String traceId = MDC.get(TRACE_ID_KEY);
        String spanId = MDC.get(SPAN_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = randomHex(16);
            MDC.put(TRACE_ID_KEY, traceId);
        }
        if (spanId == null || spanId.isBlank()) {
            spanId = randomHex(8);
            MDC.put(SPAN_ID_KEY, spanId);
        }
        return traceparent(traceId, spanId);
    }

    public static String outboundTraceparent() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = randomHex(16);
            MDC.put(TRACE_ID_KEY, traceId);
        }
        return traceparent(traceId, randomHex(8));
    }

    public static String currentRequestId() {
        String requestId = MDC.get(REQUEST_ID_KEY);
        if (requestId == null || requestId.isBlank()) {
            requestId = MDC.get(TRACE_ID_KEY);
        }
        return requestId;
    }

    public static void applyToMessageProperties(MessageProperties properties) {
        properties.setHeader(TRACEPARENT_HEADER, currentTraceparent());
        String requestId = currentRequestId();
        if (requestId != null && !requestId.isBlank()) {
            properties.setHeader(REQUEST_ID_HEADER, requestId);
        }
    }

    public static void applyMissingToMessageProperties(MessageProperties properties) {
        if (properties.getHeader(TRACEPARENT_HEADER) == null) {
            properties.setHeader(TRACEPARENT_HEADER, currentTraceparent());
        }
        if (properties.getHeader(REQUEST_ID_HEADER) == null) {
            String requestId = currentRequestId();
            if (requestId != null && !requestId.isBlank()) {
                properties.setHeader(REQUEST_ID_HEADER, requestId);
            }
        }
    }

    public static TraceHeaders fromMessageHeaders(Map<String, Object> headers) {
        Object traceparent = headers.get(TRACEPARENT_HEADER);
        Object requestId = headers.get(REQUEST_ID_HEADER);
        return fromHeaders(traceparent == null ? null : traceparent.toString(),
                requestId == null ? null : requestId.toString());
    }

    public static String traceparent(String traceId, String spanId) {
        return "00-" + traceId + "-" + spanId + "-01";
    }

    private static String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        StringBuilder value = new StringBuilder(bytes * 2);
        for (byte b : buffer) {
            value.append(String.format("%02x", b));
        }
        return value.toString();
    }

    public record TraceHeaders(String traceId, String spanId, String requestId) {
        public String traceparent() {
            return CorrelationContext.traceparent(traceId, spanId);
        }
    }
}
