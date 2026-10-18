package com.cinx.common.exception;

import com.cinx.common.logging.CorrelationContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

public final class ProblemDetailFactory {
    private ProblemDetailFactory() {
    }

    public static ProblemDetail create(HttpStatus status, ErrorCode code, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create("urn:cinx:problem:" + toKebabCase(code.name())));
        problemDetail.setTitle(code.title());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        addExtensions(problemDetail, code);
        return problemDetail;
    }

    public static ProblemDetail validation(String detail, List<FieldValidationError> errors, HttpServletRequest request) {
        ProblemDetail problemDetail = create(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, detail, request);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    private static void addExtensions(ProblemDetail problemDetail, ErrorCode code) {
        problemDetail.setProperty("code", code.name());
        problemDetail.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        problemDetail.setProperty("traceId", traceId());
    }

    public static String traceId() {
        String traceId = MDC.get(CorrelationContext.TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            CorrelationContext.TraceHeaders headers = CorrelationContext.fromHeaders(null, null);
            CorrelationContext.put(headers);
            traceId = headers.traceId();
        }
        return traceId;
    }

    private static String toKebabCase(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
