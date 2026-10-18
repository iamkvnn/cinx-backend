package com.cinx.common.exception;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailResponseWriterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesMvcSecurityProblemJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/courses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ProblemDetailResponseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "Please login and try again");

        Map<String, Object> json = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(json).containsEntry("type", "urn:cinx:problem:unauthorized");
        assertThat(json).containsEntry("title", "Unauthorized");
        assertThat(json).containsEntry("status", 401);
        assertThat(json).containsEntry("detail", "Please login and try again");
        assertThat(json).containsEntry("instance", "/api/v1/courses");
        assertThat(json).containsEntry("code", "UNAUTHORIZED");
        assertThat(json).containsKeys("timestamp", "traceId");
    }
}
