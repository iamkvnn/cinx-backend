package com.cinx.learning.config;

import com.cinx.common.security.CinxServiceTokenRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/** Service token for the assistant's internal API; its audience is the assistant's alone. */
public class AssistantFeignConfig {
    @Bean
    RequestInterceptor assistantServiceTokenInterceptor(
            RestClient.Builder builder,
            @Value("${cinx.security.authorization-freshness.token-uri:http://keycloak:8080/realms/cinx/protocol/openid-connect/token}") String tokenUri,
            @Value("${cinx.security.authorization-freshness.client-id:cinx-learning-service}") String clientId,
            @Value("${cinx.security.authorization-freshness.client-secret:}") String clientSecret) {
        return new CinxServiceTokenRequestInterceptor(builder, tokenUri, clientId, clientSecret,
                "cinx-assistant-internal", Clock.systemUTC());
    }
}
