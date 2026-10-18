package com.cinx.common.logging;

import com.cinx.common.config.FeignCorrelationConfig;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class FeignCorrelationConfigTest {

    @AfterEach
    void tearDown() {
        CorrelationContext.clear();
    }

    @Test
    void interceptorRelaysCurrentTraceAndRequestId() {
        MDC.put(CorrelationContext.TRACE_ID_KEY, "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put(CorrelationContext.SPAN_ID_KEY, "00f067aa0ba902b7");
        MDC.put(CorrelationContext.REQUEST_ID_KEY, "req-123");

        RequestTemplate template = new RequestTemplate();
        new FeignCorrelationConfig().correlationRequestInterceptor().apply(template);

        assertThat(template.headers().get(CorrelationContext.TRACEPARENT_HEADER))
                .singleElement()
                .asString()
                .startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-")
                .endsWith("-01");
        assertThat(template.headers().get(CorrelationContext.REQUEST_ID_HEADER))
                .containsExactly("req-123");
    }

    @Test
    void interceptorCreatesTraceWhenMissing() {
        RequestTemplate template = new RequestTemplate();

        new FeignCorrelationConfig().correlationRequestInterceptor().apply(template);

        assertThat(template.headers().get(CorrelationContext.TRACEPARENT_HEADER))
                .singleElement()
                .asString()
                .matches("^00-[0-9a-f]{32}-[0-9a-f]{16}-01$");
    }
}
