package com.cinx.reporting.analytics.api;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ProblemDetailFactory;
import com.cinx.reporting.analytics.application.EventRateLimitedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 429 for a client over its hourly event budget; ordered first, since the shared handler turns anything unknown into 500. */
@RestControllerAdvice(assignableTypes = ProductEventController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AnalyticsExceptionHandler {
    @ExceptionHandler(EventRateLimitedException.class)
    public ResponseEntity<ProblemDetail> rateLimited(EventRateLimitedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "3600")
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetailFactory.create(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.QUOTA_EXCEEDED, ex.getMessage(), request));
    }
}
