package com.cinx.learning.config;

import com.cinx.common.security.CinxServiceTokenRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.time.Clock;

public class UserServiceFeignConfig {
    @Bean
    RequestInterceptor userServiceTokenInterceptor(
            RestClient.Builder builder,
            @Value("${cinx.security.authorization-freshness.token-uri:http://keycloak:8080/realms/cinx/protocol/openid-connect/token}") String tokenUri,
            @Value("${cinx.security.authorization-freshness.client-id:cinx-learning-service}") String clientId,
            @Value("${cinx.security.authorization-freshness.client-secret:}") String clientSecret) {
        return new CinxServiceTokenRequestInterceptor(builder, tokenUri, clientId, clientSecret,
                "cinx-user-internal", Clock.systemUTC());
    }
}
