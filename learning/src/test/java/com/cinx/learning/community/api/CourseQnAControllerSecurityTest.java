package com.cinx.learning.community.api;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.learning.community.api.mapper.CommunityApiMapper;
import com.cinx.learning.community.application.ICourseQnAService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseQnAController.class)
@Import(CourseQnAControllerSecurityTest.SecurityTestConfig.class)
class CourseQnAControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICourseQnAService qnaService;

    @MockBean
    private CommunityApiMapper communityApiMapper;

    @Test
    void unauthenticatedHelpfulMarkIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/community/qna/answers/answer-1/helpful"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(qnaService);
    }

    @Test
    void authenticatedHelpfulMarkUsesJwtIdentity() throws Exception {
        mockMvc.perform(put("/api/v1/community/qna/answers/answer-1/helpful")
                        .with(jwt().jwt(jwt -> jwt.subject("jwt-user"))))
                .andExpect(status().isOk());

        verify(qnaService).markAnswerHelpful("jwt-user", "answer-1");
    }

    @Test
    void unauthenticatedSimilarQuestionsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/community/qna/questions/similar")
                        .param("courseId", "course-1").param("text", "range(5) thiếu số 5"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(qnaService);
    }

    @Test
    void outsiderOfTheCourseGetsForbiddenWithTheEnrollmentCode() throws Exception {
        when(qnaService.getQuestionsByCourse(eq("course-1"), any(), eq("outsider"), anyInt(), anyInt(), any(), any(), anyBoolean()))
                .thenThrow(new ForbiddenException(ErrorCode.NOT_ENROLLED_IN_COURSE, "Not enrolled in this course"));

        mockMvc.perform(get("/api/v1/community/qna/questions").param("courseId", "course-1")
                        .with(jwt().jwt(jwt -> jwt.subject("outsider"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ENROLLED_IN_COURSE"));
    }

    @TestConfiguration
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
