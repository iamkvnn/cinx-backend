package com.cinx.user.config;

import com.cinx.common.security.CinxJwtAuthenticationConverter;
import com.cinx.common.security.KeycloakJwtDecoderFactory;
import com.cinx.common.security.AuthorizationFreshnessConfiguration;
import com.cinx.common.security.AuthorizationFreshnessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.http.HttpMethod;

import java.util.Set;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(AuthorizationFreshnessConfiguration.class)
public class SecurityConfig {
    @Value("${security.keycloak.issuer-uri:http://localhost:8180/realms/cinx}")
    private String keycloakIssuer;
    @Value("${security.keycloak.jwk-set-uri:http://localhost:8180/realms/cinx/protocol/openid-connect/certs}")
    private String keycloakJwkSetUri;
    @Value("${security.keycloak.audience:cinx-api}")
    private String keycloakAudience;

    private final JWTAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JWTAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/internal/users/*", "/internal/user-ids",
                                "/internal/users/*/instructor-verified").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(internalJwtDecoder())
                                .jwtAuthenticationConverter(new CinxJwtAuthenticationConverter(
                                        "cinx-user-internal", Set.of())))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthorizationFreshnessFilter freshnessFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
             .authorizeHttpRequests(
                auth -> auth
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/policies/versions").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/policies/detail/*").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/policies", "/api/v1/policies/*").permitAll()
                    .anyRequest().authenticated()
             )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterAfter(freshnessFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CinxJwtAuthenticationConverter jwtAuthenticationConverter(){
        return new CinxJwtAuthenticationConverter();
    }

    @Bean
    public JwtDecoder jwtDecoder(){
        return KeycloakJwtDecoderFactory.create(keycloakIssuer, keycloakJwkSetUri, keycloakAudience);
    }

    JwtDecoder internalJwtDecoder() {
        return KeycloakJwtDecoderFactory.create(keycloakIssuer, keycloakJwkSetUri,
                "cinx-user-internal", "service",
                Set.of("cinx-learning-service", "cinx-notification-service"));
    }
}
