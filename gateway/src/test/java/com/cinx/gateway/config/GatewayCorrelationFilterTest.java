package com.cinx.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayCorrelationFilterTest {
    private final GatewayCorrelationFilter filter = new GatewayCorrelationFilter();

    @Test
    void forwardsIncomingTraceparentAndRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/profiles/me")
                .header(GatewayCorrelationFilter.TRACEPARENT_HEADER,
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
                .header(GatewayCorrelationFilter.REQUEST_ID_HEADER, "request-789")
                .build());

        GatewayFilterChain chain = filteredExchange -> {
            assertThat(filteredExchange.getRequest().getHeaders().getFirst(GatewayCorrelationFilter.TRACEPARENT_HEADER))
                    .startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-")
                    .endsWith("-01");
            assertThat(filteredExchange.getRequest().getHeaders().getFirst(GatewayCorrelationFilter.REQUEST_ID_HEADER))
                    .isEqualTo("request-789");
            filteredExchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(GatewayCorrelationFilter.TRACEPARENT_HEADER))
                .startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-")
                .endsWith("-01");
        assertThat(exchange.getResponse().getHeaders().getFirst(GatewayCorrelationFilter.REQUEST_ID_HEADER))
                .isEqualTo("request-789");
    }
}
