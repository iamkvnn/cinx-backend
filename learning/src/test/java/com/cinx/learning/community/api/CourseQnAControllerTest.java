package com.cinx.learning.community.api;

import com.cinx.learning.community.application.ICourseQnAService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseQnAControllerTest {
    @Mock
    private ICourseQnAService qnaService;

    @InjectMocks
    private CourseQnAController controller;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markHelpfulUsesUidFromJwt() {
        authenticateAs("jwt-user");

        var response = controller.markAnswerHelpful("answer-1");

        verify(qnaService).markAnswerHelpful("jwt-user", "answer-1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
    }

    @Test
    void unmarkHelpfulUsesUidFromJwt() {
        authenticateAs("jwt-user");

        var response = controller.unmarkAnswerHelpful("answer-1");

        verify(qnaService).unmarkAnswerHelpful("jwt-user", "answer-1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
    }

    private void authenticateAs(String userId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
