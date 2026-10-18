package com.cinx.common.config;

import com.cinx.common.logging.CorrelationContext;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor correlationRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header(CorrelationContext.TRACEPARENT_HEADER, CorrelationContext.outboundTraceparent());
            String requestId = CorrelationContext.currentRequestId();
            if (requestId != null && !requestId.isBlank()) {
                requestTemplate.header(CorrelationContext.REQUEST_ID_HEADER, requestId);
            }
        };
    }
}
