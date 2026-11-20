package com.cinx.gateway.config;

import com.cinx.common.security.CinxJwtAuthenticationConverter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import java.time.Clock;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Value("${security.keycloak.issuer-uri:http://localhost:8180/realms/cinx}")
    private String keycloakIssuer;
    @Value("${security.keycloak.jwk-set-uri:http://localhost:8180/realms/cinx/protocol/openid-connect/certs}")
    private String keycloakJwkSetUri;
    @Value("${security.keycloak.audience:cinx-api}")
    private String keycloakAudience;

    @Value("${cors.allowed-origins:http://localhost:*}")
    private List<String> allowedOrigins;

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveStringRedisTemplate redis) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/internal/**").denyAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                .pathMatchers("/realms/**", "/resources/**").permitAll()
                .pathMatchers(HttpMethod.GET,
                        "/api/v1/courses/mine",
                        "/api/v1/courses/upload",
                        "/api/v1/courses/mine",
                        "/api/v1/policies/versions").authenticated()
                .pathMatchers(HttpMethod.GET,
                        "/api/v1/courses",
                        "/api/v1/courses/{courseId}",
                        "/api/v1/instructors/{instructorId}",
                        "/api/v1/courses/ids",
                        "/api/v1/courses/*/curriculum",
                        "/api/v1/courses/*/lessons/*/articles",
                        "/api/v1/courses/*/lessons/*/videos",
                        "/api/v1/courses/categories/**",
                        "/api/v1/courses/stats/public",
                        "/api/v1/learning/featured-paths",
                        "/api/v1/learning/featured-paths/*",
                        "/api/v1/assessments/daily-quiz",
                        "/api/v1/community/reviews/**",
                        "/api/v1/policies",
                        "/api/v1/policies/*",
                        "/api/v1/plans",
                        "/api/v1/plans/*",
                        "/api/v1/recommendations/search").permitAll()
                // campus checks the bearer token itself when a seat is reserved; the room socket that follows carries none
                // a lab machine's socket carries a ticket in its URL, which the runner verifies before it upgrades
                .pathMatchers("/api/v1/payments/webhooks/**",
                        "/ws/**", "/sockjs/**", "/campus/**", "/lab-sessions/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwtDecoder(reactiveJwtDecoder())
                        .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(
                                new CinxJwtAuthenticationConverter())))
                .authenticationEntryPoint((exchange, ex) -> GatewayProblemDetailWriter.write(
                         exchange,
                         org.springframework.http.HttpStatus.UNAUTHORIZED,
                         "UNAUTHORIZED",
                         "Unauthorized",
                         "Please login and try again"))
                .accessDeniedHandler((exchange, ex) -> GatewayProblemDetailWriter.write(
                         exchange,
                         org.springframework.http.HttpStatus.FORBIDDEN,
                         "FORBIDDEN",
                         "Forbidden",
                         "Access denied"))
            ).cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterAfter(new AuthorizationFreshnessWebFilter(redis, Clock.systemUTC()), SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder() {
        return KeycloakReactiveJwtDecoder.create(keycloakIssuer, keycloakJwkSetUri, keycloakAudience);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);
        // the campus SDK's matchmaking requests (the room socket that follows is not subject to CORS)
        source.registerCorsConfiguration("/campus/**", config);
        source.registerCorsConfiguration("/lab-sessions/**", config);
        return source;
    }
}
