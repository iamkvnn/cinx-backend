package com.cinx.reporting.config;

import com.cinx.common.security.CinxJwtAuthenticationConverter;
import com.cinx.common.security.KeycloakJwtDecoderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import java.util.Set;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${security.keycloak.issuer-uri:http://localhost:8180/realms/cinx}")
    private String keycloakIssuer;
    @Value("${security.keycloak.jwk-set-uri:http://localhost:8180/realms/cinx/protocol/openid-connect/certs}")
    private String keycloakJwkSetUri;
    @Value("${security.keycloak.audience:cinx-api}")
    private String keycloakAudience;

    /** Service-to-service reads (the assistant's admin metrics); only service tokens of allowed clients. */
    @Bean
    @Order(1)
    SecurityFilterChain internalSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/internal/reporting/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(KeycloakJwtDecoderFactory.create(keycloakIssuer, keycloakJwkSetUri,
                                "cinx-reporting-internal", "service", Set.of("cinx-assistant-service")))
                        .jwtAuthenticationConverter(new CinxJwtAuthenticationConverter("cinx-reporting-internal", Set.of()))))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(new CinxJwtAuthenticationConverter())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return KeycloakJwtDecoderFactory.create(keycloakIssuer, keycloakJwkSetUri, keycloakAudience);
    }
}
