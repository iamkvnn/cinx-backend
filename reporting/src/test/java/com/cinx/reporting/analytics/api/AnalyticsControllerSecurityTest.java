package com.cinx.reporting.analytics.api;

import com.cinx.common.security.CinxMfaVerifier;
import com.cinx.reporting.analytics.api.mapper.AnalyticsMapperImpl;
import com.cinx.reporting.analytics.application.EventRateLimitedException;
import com.cinx.reporting.analytics.application.IExperimentService;
import com.cinx.reporting.analytics.application.IFunnelService;
import com.cinx.reporting.analytics.application.IProductEventService;
import com.cinx.reporting.analytics.application.query.AssignmentView;
import com.cinx.reporting.analytics.application.query.IngestResult;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductEventController.class, ExperimentController.class, FunnelController.class})
@Import({AnalyticsMapperImpl.class, CinxMfaVerifier.class, AnalyticsControllerSecurityTest.SecurityTestConfig.class})
class AnalyticsControllerSecurityTest {
    private static final String BATCH = """
            {"events":[{"eventId":"3f1c2a9e-8f7b-4c2d-9a51-0d4b7e6f1a20","name":"recommendation_clicked",
            "occurredAt":"2026-10-13T09:00:00+07:00","props":{"recommendationId":"r1","courseId":"c1","userId":"someone-else"}}]}""";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private IProductEventService productEvents;
    @MockBean
    private IExperimentService experiments;
    @MockBean
    private IFunnelService funnels;

    @Test
    void everyRouteNeedsAToken() throws Exception {
        mockMvc.perform(post("/api/v1/reporting/events").contentType(MediaType.APPLICATION_JSON).content(BATCH))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reporting/experiments/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reporting/funnels?steps=a,b&from=2026-10-01&to=2026-10-13")).andExpect(status().isUnauthorized());
        verifyNoInteractions(productEvents, experiments, funnels);
    }

    @Test
    void aLearnerRecordsEventsAsTheTokensUserAndReadsTheirAssignments() throws Exception {
        when(productEvents.ingest(eq("u1"), anyList())).thenReturn(new IngestResult(1, 0, 0));
        when(experiments.assignments("u1")).thenReturn(List.of(new AssignmentView("week_review_card", "hidden")));

        mockMvc.perform(post("/api/v1/reporting/events").contentType(MediaType.APPLICATION_JSON).content(BATCH).with(learner()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.accepted").value(1));
        mockMvc.perform(get("/api/v1/reporting/experiments/me").with(learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].variant").value("hidden"));
        verify(productEvents).ingest(eq("u1"), anyList());
    }

    @Test
    void anEmptyBatchIsRefusedAndTooManyEventsGet429() throws Exception {
        mockMvc.perform(post("/api/v1/reporting/events").contentType(MediaType.APPLICATION_JSON).content("{\"events\":[]}").with(learner()))
                .andExpect(status().isBadRequest());
        when(productEvents.ingest(eq("u1"), anyList())).thenThrow(new EventRateLimitedException());
        mockMvc.perform(post("/api/v1/reporting/events").contentType(MediaType.APPLICATION_JSON).content(BATCH).with(learner()))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void aLearnerCannotManageOrReadExperimentsAndFunnels() throws Exception {
        mockMvc.perform(post("/api/v1/reporting/experiments/week_review_card/stop").with(learner())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reporting/experiments").with(learner())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reporting/funnels?steps=a,b&from=2026-10-01&to=2026-10-13").with(learner()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(experiments, funnels);
    }

    @Test
    void theRightPermissionStillNeedsMfa() throws Exception {
        mockMvc.perform(post("/api/v1/reporting/experiments/week_review_card/stop")
                        .with(jwt().jwt(j -> j.subject("a1")).authorities(new SimpleGrantedAuthority("PERM_reporting.experiment.manage.any"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MFA_STEP_UP_REQUIRED"));
        verifyNoInteractions(experiments);
    }

    @Test
    void anAnalystWithMfaStopsAnExperiment() throws Exception {
        mockMvc.perform(post("/api/v1/reporting/experiments/week_review_card/stop").with(analystWithMfa()))
                .andExpect(status().isOk());
        verify(experiments).stop("week_review_card", "a1");
    }

    private static RequestPostProcessor learner() {
        return jwt().jwt(j -> j.subject("u1")).authorities(new SimpleGrantedAuthority("PERM_learning.progress.read.self"));
    }

    private static RequestPostProcessor analystWithMfa() {
        return jwt().jwt(j -> j.subject("a1").claim("acr", CinxMfaVerifier.REQUIRED_ACR).claim("amr", List.of("otp"))
                        .claim("auth_time", Instant.now().getEpochSecond()))
                .authorities(new SimpleGrantedAuthority("PERM_reporting.experiment.manage.any"));
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
