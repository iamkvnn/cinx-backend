package com.cinx.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class GatewayCorrelationFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GatewayCorrelationFilter.class);
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_ATTRIBUTE = "cinx.traceId";

    private static final Pattern TRACEPARENT_PATTERN =
            Pattern.compile("^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        TraceHeaders headers = traceHeaders(exchange);
        long startedAt = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(TRACEPARENT_HEADER, headers.traceparent())
                .header(REQUEST_ID_HEADER, headers.requestId())
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getAttributes().put(TRACE_ID_ATTRIBUTE, headers.traceId());
        mutatedExchange.getResponse().getHeaders().set(TRACEPARENT_HEADER, headers.traceparent());
        mutatedExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, headers.requestId());

        putMdc(headers, request);
        return chain.filter(mutatedExchange)
                .doOnError(error -> log.error("Gateway request failed", error))
                .doFinally(signalType -> {
                    putMdc(headers, request);
                    long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
                    Integer status = mutatedExchange.getResponse().getStatusCode() == null
                            ? null
                            : mutatedExchange.getResponse().getStatusCode().value();
                    String routeId = routeId(mutatedExchange);
                    MDC.put("status", status == null ? "" : status.toString());
                    MDC.put("durationMs", Long.toString(durationMs));
                    log.info("Gateway request completed method={} path={} routeId={} status={} durationMs={}",
                            request.getMethod(), request.getURI().getRawPath(), routeId, status, durationMs);
                    clearMdc();
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private TraceHeaders traceHeaders(ServerWebExchange exchange) {
        String incomingTraceparent = exchange.getRequest().getHeaders().getFirst(TRACEPARENT_HEADER);
        String traceId;
        if (incomingTraceparent != null
                && TRACEPARENT_PATTERN.matcher(incomingTraceparent.trim().toLowerCase(Locale.ROOT)).matches()) {
            traceId = incomingTraceparent.trim().substring(3, 35).toLowerCase(Locale.ROOT);
        } else {
            traceId = randomHex(16);
        }
        String spanId = randomHex(8);
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = traceId;
        }
        return new TraceHeaders(traceId, spanId, requestId);
    }

    private void putMdc(TraceHeaders headers, ServerHttpRequest request) {
        MDC.put("traceId", headers.traceId());
        MDC.put("spanId", headers.spanId());
        MDC.put("requestId", headers.requestId());
        MDC.put("method", request.getMethod() == null ? "" : request.getMethod().name());
        MDC.put("path", request.getURI().getRawPath());
    }

    private void clearMdc() {
        MDC.remove("traceId");
        MDC.remove("spanId");
        MDC.remove("requestId");
        MDC.remove("method");
        MDC.remove("path");
        MDC.remove("status");
        MDC.remove("durationMs");
    }

    private String routeId(ServerWebExchange exchange) {
        Object route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route == null ? null : route.toString();
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

    private record TraceHeaders(String traceId, String spanId, String requestId) {
        private String traceparent() {
            return "00-" + traceId + "-" + spanId + "-01";
        }
    }
}
