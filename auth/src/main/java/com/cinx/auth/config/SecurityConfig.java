package com.cinx.auth.config;

import com.cinx.common.security.CinxJwtAuthenticationConverter;
import com.cinx.common.security.KeycloakJwtDecoderFactory;
import com.cinx.common.security.AuthorizationFreshnessFilter;
import com.cinx.common.security.AuthorizationFreshnessVerifier;
import com.cinx.common.security.AuthorizationState;
import com.cinx.auth.identity.application.IIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import java.util.Set;
import java.time.Clock;
import java.time.Duration;

@RequiredArgsConstructor
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${security.keycloak.issuer-uri:http://localhost:8180/realms/cinx}") private String issuer;
    @Value("${security.keycloak.jwk-set-uri:http://localhost:8180/realms/cinx/protocol/openid-connect/certs}") private String jwks;
    @Value("${security.keycloak.audience:cinx-api}") private String userAudience;
    private final JWTAuthenticationEntryPoint entryPoint;
    private final JWTAccessDeniedHandler deniedHandler;

    @Bean
    @Order(1)
    SecurityFilterChain internalSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                                .decoder(internalDecoder())
                                .jwtAuthenticationConverter(new CinxJwtAuthenticationConverter("cinx-auth-internal", Set.of())))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain userSecurity(HttpSecurity http, AuthorizationFreshnessFilter freshnessFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(userDecoder())
                                .jwtAuthenticationConverter(new CinxJwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterAfter(freshnessFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    AuthorizationFreshnessVerifier authorizationFreshnessVerifier(IIdentityService identities, Clock clock) {
        return new AuthorizationFreshnessVerifier(userId -> {
            var state = identities.authorizationState(userId);
            return new AuthorizationState(state.userId(), state.status().name(), state.authorizationVersion(),
                    state.authorizationPolicyVersion(), state.evaluatedAt());
        }, clock, Duration.ZERO);
    }

    @Bean
    AuthorizationFreshnessFilter authorizationFreshnessFilter(AuthorizationFreshnessVerifier verifier) {
        return new AuthorizationFreshnessFilter(verifier);
    }

    JwtDecoder userDecoder() {
        return KeycloakJwtDecoderFactory.create(issuer, jwks, userAudience);
    }

    JwtDecoder internalDecoder() {
        return KeycloakJwtDecoderFactory.create(issuer, jwks, "cinx-auth-internal", "service",
                Set.of("cinx-gateway-service", "cinx-user-service", "cinx-commerce-service",
                        "cinx-learning-service", "cinx-notification-service", "cinx-reporting-service",
                        "cinx-recommendation-service", "cinx-assistant-service"));
    }
}
