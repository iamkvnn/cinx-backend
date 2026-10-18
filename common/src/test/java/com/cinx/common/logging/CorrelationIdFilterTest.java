package com.cinx.common.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void createsTraceHeadersWhenMissingAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profiles/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationContext.TRACEPARENT_HEADER))
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01");
        assertThat(response.getHeader(CorrelationContext.REQUEST_ID_HEADER))
                .matches("[0-9a-f]{32}");
        assertThat(MDC.get(CorrelationContext.TRACE_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationContext.REQUEST_ID_KEY)).isNull();
    }

    @Test
    void preservesIncomingTraceIdAndRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/courses");
        request.addHeader(CorrelationContext.TRACEPARENT_HEADER,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        request.addHeader(CorrelationContext.REQUEST_ID_HEADER, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationContext.TRACEPARENT_HEADER))
                .startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-")
                .endsWith("-01");
        assertThat(response.getHeader(CorrelationContext.REQUEST_ID_HEADER)).isEqualTo("request-123");
        assertThat(MDC.get(CorrelationContext.TRACE_ID_KEY)).isNull();
    }
}
