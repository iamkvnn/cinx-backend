package com.cinx.gateway.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayProblemDetailWriterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesProblemJsonContract() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/orders").build());

        GatewayProblemDetailWriter.write(
                exchange,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Forbidden",
                "Access denied").block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        String body = exchange.getResponse().getBodyAsString().block();
        Map<String, Object> json = objectMapper.readValue(body, new TypeReference<>() {
        });

        assertThat(json).containsEntry("type", "urn:cinx:problem:forbidden");
        assertThat(json).containsEntry("title", "Forbidden");
        assertThat(json).containsEntry("status", 403);
        assertThat(json).containsEntry("detail", "Access denied");
        assertThat(json).containsEntry("instance", "/internal/orders");
        assertThat(json).containsEntry("code", "FORBIDDEN");
        assertThat(json).containsKeys("timestamp", "traceId");
    }
}
