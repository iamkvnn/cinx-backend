package com.cinx.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProblemDetailResponseWriter {
    private ProblemDetailResponseWriter() {
    }

    public static void write(HttpServletRequest request, HttpServletResponse response,
                             HttpStatus status, ErrorCode code, String detail) throws IOException {
        ProblemDetail problemDetail = ProblemDetailFactory.create(status, code, detail, request);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        new ObjectMapper().writeValue(response.getWriter(), toJsonBody(problemDetail));
    }

    private static Map<String, Object> toJsonBody(ProblemDetail problemDetail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problemDetail.getType().toString());
        body.put("title", problemDetail.getTitle());
        body.put("status", problemDetail.getStatus());
        body.put("detail", problemDetail.getDetail());
        body.put("instance", problemDetail.getInstance().toString());
        if (problemDetail.getProperties() != null) {
            body.putAll(problemDetail.getProperties());
        }
        return body;
    }
}
