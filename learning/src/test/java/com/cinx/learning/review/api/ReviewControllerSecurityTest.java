package com.cinx.learning.review.api;

import com.cinx.learning.review.application.IReviewService;
import com.cinx.learning.review.application.query.ReviewAnswerView;
import com.cinx.learning.review.application.query.ReviewTodayView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@Import(ReviewControllerSecurityTest.SecurityTestConfig.class)
class ReviewControllerSecurityTest {
    private static final String ANSWER = "/api/v1/learning/reviews/r1/attempts";
    private static final String BODY = "{\"userAnswer\":\"[\\\"a\\\"]\",\"elapsedSeconds\":12}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IReviewService reviews;

    @Test
    void everyCallNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/v1/learning/reviews/today")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ANSWER).header("Idempotency-Key", "k1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(reviews);
    }

    @Test
    void answeringNeedsTheWritePermission() throws Exception {
        mockMvc.perform(post(ANSWER).header("Idempotency-Key", "k1").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_learning.progress.read.self"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reviews);
    }

    @Test
    void theQueueAndTheAnswerAreTheTokensOwn() throws Exception {
        when(reviews.today("u1")).thenReturn(new ReviewTodayView(0, 20, List.of()));
        when(reviews.answer("u1", "r1", "[\"a\"]", 12, "k1")).thenReturn(new ReviewAnswerView(true, 1.0, "[\"a\"]", null, 2, null, false));

        mockMvc.perform(get("/api/v1/learning/reviews/today")
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_learning.progress.read.self"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyCap").value(20));
        mockMvc.perform(post(ANSWER).header("Idempotency-Key", "k1").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_learning.progress.write.self"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.box").value(2));
        verify(reviews).answer("u1", "r1", "[\"a\"]", 12, "k1");
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
            return http.build();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("JWT decoder is bypassed by jwt() test authentication");
            };
        }
    }
}
