package com.cinx.commerce.config;

import com.cinx.common.security.CinxJwtAuthenticationConverter;
import com.cinx.common.security.KeycloakJwtDecoderFactory;
import com.cinx.common.security.AuthorizationFreshnessConfiguration;
import com.cinx.common.security.AuthorizationFreshnessFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Set;

@Configuration
@EnableMethodSecurity
@Import(AuthorizationFreshnessConfiguration.class)
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${security.keycloak.issuer-uri}") String issuer,
                          @Value("${security.keycloak.jwk-set-uri}") String jwks,
                          @Value("${security.keycloak.audience:cinx-api}") String audience) {
        return KeycloakJwtDecoderFactory.create(issuer, jwks, audience);
    }

    @Bean
    @Order(1)
    SecurityFilterChain internalSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.requestMatchers("/internal/entitlements/users/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(internalJwtDecoder())
                        .jwtAuthenticationConverter(new CinxJwtAuthenticationConverter(
                                "cinx-commerce-internal", Set.of("cinx-learning-service", "cinx-recommendation-service",
                                "cinx-assistant-service")))))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    JwtDecoder internalJwtDecoder() {
        return KeycloakJwtDecoderFactory.create(issuerForInternal, jwksForInternal,
                "cinx-commerce-internal", "service",
                Set.of("cinx-learning-service", "cinx-recommendation-service", "cinx-assistant-service"));
    }

    @Value("${security.keycloak.issuer-uri:http://localhost:8180/realms/cinx}")
    private String issuerForInternal;

    @Value("${security.keycloak.jwk-set-uri:http://localhost:8180/realms/cinx/protocol/openid-connect/certs}")
    private String jwksForInternal;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder, AuthorizationFreshnessFilter freshnessFilter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.requestMatchers("/actuator/**", "/api/v1/payments/webhooks/**",
                                "/api/v1/billing/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/plans", "/api/v1/plans/*").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder)
                        .jwtAuthenticationConverter(new CinxJwtAuthenticationConverter())))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(freshnessFilter, BearerTokenAuthenticationFilter.class).build();
    }
}
