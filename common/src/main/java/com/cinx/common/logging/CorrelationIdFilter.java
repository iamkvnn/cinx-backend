package com.cinx.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CorrelationContext.TraceHeaders traceHeaders = CorrelationContext.fromHeaders(
                request.getHeader(CorrelationContext.TRACEPARENT_HEADER),
                request.getHeader(CorrelationContext.REQUEST_ID_HEADER));
        long startedAt = System.nanoTime();
        CorrelationContext.put(traceHeaders);
        MDC.put(CorrelationContext.METHOD_KEY, request.getMethod());
        MDC.put(CorrelationContext.PATH_KEY, request.getRequestURI());
        response.setHeader(CorrelationContext.TRACEPARENT_HEADER, traceHeaders.traceparent());
        response.setHeader(CorrelationContext.REQUEST_ID_HEADER, traceHeaders.requestId());

        try {
            filterChain.doFilter(request, response);
        } finally {
            putUserIdIfAvailable();
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            MDC.put(CorrelationContext.STATUS_KEY, Integer.toString(response.getStatus()));
            MDC.put(CorrelationContext.DURATION_MS_KEY, Long.toString(durationMs));
            log.info("HTTP request completed method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            CorrelationContext.clear();
        }
    }

    private void putUserIdIfAvailable() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !authentication.getName().isBlank()) {
            MDC.put(CorrelationContext.USER_ID_KEY, authentication.getName());
        }
    }
}
