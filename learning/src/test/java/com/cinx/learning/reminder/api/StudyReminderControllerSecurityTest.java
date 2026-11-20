package com.cinx.learning.reminder.api;

import com.cinx.learning.reminder.application.StudyReminderPreferenceService;
import com.cinx.learning.reminder.domain.StudyReminderPreference;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyReminderController.class)
@Import(StudyReminderControllerSecurityTest.SecurityTestConfig.class)
class StudyReminderControllerSecurityTest {
    private static final String BODY = "{\"enabled\":false,\"hour\":8}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudyReminderPreferenceService preferences;

    @Test
    void unauthenticatedCallsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/learning/study-reminder")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/learning/study-reminder").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(preferences);
    }

    @Test
    void writingNeedsTheWritePermission() throws Exception {
        mockMvc.perform(put("/api/v1/learning/study-reminder").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_learning.progress.read.self"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(preferences);
    }

    @Test
    void theLearnersOwnPreferenceIsReadFromTheToken() throws Exception {
        when(preferences.get("u1")).thenReturn(StudyReminderPreference.defaultFor("u1"));

        mockMvc.perform(get("/api/v1/learning/study-reminder")
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_learning.progress.read.self"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hour").value(19));
        verify(preferences).get("u1");
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
