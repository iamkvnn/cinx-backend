package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;
import com.cinx.learning.catalog.application.ISubtitleJobService;
import com.cinx.learning.catalog.application.ISubtitleTrackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubtitleTrackController.class)
@Import(SubtitleTrackControllerSecurityTest.SecurityTestConfig.class)
class SubtitleTrackControllerSecurityTest {
    private static final String APPROVE = "/api/v1/courses/c1/lessons/l1/videos/subtitles/s1/approve";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISubtitleTrackService subtitleTrackService;
    @MockBean
    private ISubtitleJobService subtitleJobService;
    @MockBean
    private CatalogApiMapper catalogApiMapper;

    @Test
    void approvingNeedsAToken() throws Exception {
        mockMvc.perform(post(APPROVE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(subtitleTrackService);
    }

    @Test
    void approvingNeedsTheCurriculumPermission() throws Exception {
        mockMvc.perform(post(APPROVE)
                        .with(jwt().jwt(j -> j.subject("learner-1")).authorities(new SimpleGrantedAuthority("PERM_learning.progress.read.self"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(subtitleTrackService);
    }

    @Test
    void theReviewerIsTakenFromTheToken() throws Exception {
        mockMvc.perform(post(APPROVE)
                        .with(jwt().jwt(j -> j.subject("instructor-1")).authorities(new SimpleGrantedAuthority("PERM_learning.curriculum.update.own"))))
                .andExpect(status().isOk());
        verify(subtitleTrackService).approveSubtitle("instructor-1", "c1", "l1", "s1");
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
