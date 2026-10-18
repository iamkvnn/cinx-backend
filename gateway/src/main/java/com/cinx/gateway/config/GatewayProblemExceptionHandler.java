package com.cinx.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayProblemExceptionHandler implements ErrorWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayProblemExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatus status = HttpStatus.valueOf(responseStatusException.getStatusCode().value());
            String code = codeFor(status);
            String detail = responseStatusException.getReason() != null
                    ? responseStatusException.getReason()
                    : status.getReasonPhrase();
            return GatewayProblemDetailWriter.write(exchange, status, code, titleFor(code, status), detail);
        }

        log.error("Unexpected gateway error", ex);
        return GatewayProblemDetailWriter.write(
                exchange,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal server error",
                "An unexpected error occurred");
    }

    private String codeFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "RESOURCE_NOT_FOUND";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            default -> status.is4xxClientError() ? "BAD_REQUEST" : "INTERNAL_ERROR";
        };
    }

    private String titleFor(String code, HttpStatus status) {
        return switch (code) {
            case "RESOURCE_NOT_FOUND" -> "Resource not found";
            case "UNAUTHORIZED" -> "Unauthorized";
            case "FORBIDDEN" -> "Forbidden";
            case "BAD_REQUEST" -> "Bad request";
            case "INTERNAL_ERROR" -> "Internal server error";
            default -> status.getReasonPhrase();
        };
    }
}
