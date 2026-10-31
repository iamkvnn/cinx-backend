package com.cinx.user.profile.api;

import com.cinx.user.profile.api.mapper.UserMapper;
import com.cinx.user.profile.application.ILearnerProfileService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearnerProfileController.class)
@Import(LearnerProfileControllerSecurityTest.SecurityTestConfig.class)
class LearnerProfileControllerSecurityTest {
    private static final String PATH = "/api/v1/profiles/me/learning-profile";
    private static final String BODY = "{\"goal\":\"SKILL_UP\",\"minutesPerDay\":30,\"categoryIds\":[\"c1\"]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ILearnerProfileService learnerProfiles;
    @MockBean
    private UserMapper userMapper;

    @Test
    void everyCallNeedsAToken() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(PATH + "/skip")).andExpect(status().isUnauthorized());
        verifyNoInteractions(learnerProfiles);
    }

    @Test
    void writingNeedsTheUpdatePermission() throws Exception {
        mockMvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_user.profile.read.self"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(PATH + "/skip")
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_user.profile.read.self"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(learnerProfiles);
    }

    @Test
    void theProfileIsAlwaysTheTokensOwn() throws Exception {
        mockMvc.perform(get(PATH).with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_user.profile.read.self"))))
                .andExpect(status().isOk());
        mockMvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_user.profile.update.self"))))
                .andExpect(status().isOk());
        verify(learnerProfiles).get("u1");
        verify(learnerProfiles).save(eq("u1"), any());
    }

    @Test
    void anUnknownGoalIsABadRequest() throws Exception {
        mockMvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"goal\":\"GET_RICH\"}")
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_user.profile.update.self"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(learnerProfiles);
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
