package com.cinx.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "cinx.security.authorization-freshness.enabled", havingValue = "true", matchIfMissing = true)
public class AuthorizationFreshnessConfiguration {
    @Bean
    AuthorizationFreshnessVerifier authorizationFreshnessVerifier(
            RestClient.Builder builder,
            @Value("${cinx.security.authorization-freshness.auth-base-url:http://auth:8888}") String authBaseUrl,
            @Value("${cinx.security.authorization-freshness.token-uri:http://keycloak:8080/realms/cinx/protocol/openid-connect/token}") String tokenUri,
            @Value("${cinx.security.authorization-freshness.client-id:unset-service-client}") String clientId,
            @Value("${cinx.security.authorization-freshness.client-secret:}") String clientSecret,
            @Value("${cinx.security.authorization-freshness.audience:cinx-auth-internal}") String audience,
            @Value("${cinx.security.authorization-freshness.maximum-age:PT5S}") Duration maximumAge) {
        Clock clock = Clock.systemUTC();
        return new AuthorizationFreshnessVerifier(new ServiceAuthorizationStateSource(
                builder, authBaseUrl, tokenUri, clientId, clientSecret, audience, clock), clock, maximumAge);
    }

    @Bean
    AuthorizationFreshnessFilter authorizationFreshnessFilter(AuthorizationFreshnessVerifier verifier) {
        return new AuthorizationFreshnessFilter(verifier);
    }
}
