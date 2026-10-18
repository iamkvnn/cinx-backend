package com.cinx.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class GatewayProblemDetailWriter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GatewayProblemDetailWriter() {
    }

    public static Mono<Void> write(ServerWebExchange exchange, HttpStatus status,
                                   String code, String title, String detail) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "urn:cinx:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-'));
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", exchange.getRequest().getPath().value());
        body.put("code", code);
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        body.put("traceId", traceId(exchange));

        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            bytes = fallbackBody(status, code, detail).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static String traceId(ServerWebExchange exchange) {
        Object traceId = exchange.getAttribute(GatewayCorrelationFilter.TRACE_ID_ATTRIBUTE);
        return traceId == null ? exchange.getRequest().getId() : traceId.toString();
    }

    private static String fallbackBody(HttpStatus status, String code, String detail) {
        return "{\"type\":\"urn:cinx:problem:internal-error\",\"title\":\""
                + status.getReasonPhrase()
                + "\",\"status\":"
                + status.value()
                + ",\"detail\":\""
                + detail
                + "\",\"code\":\""
                + code
                + "\"}";
    }
}
