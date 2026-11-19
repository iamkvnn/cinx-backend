package com.cinx.notification.config;

import com.cinx.common.logging.RabbitCorrelationAdvice;
import com.cinx.common.logging.RabbitCorrelationReceivePostProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class ConsumerConfig {
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            AmqpTemplate amqpTemplate,
            RabbitCorrelationReceivePostProcessor correlationReceivePostProcessor,
            RabbitCorrelationAdvice correlationAdvice,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAutoStartup(autoStartup);

        // Auto mode for retry to work correctly with interceptor
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        // Tune prefetch to control how many unacknowledged messages a consumer holds
        factory.setPrefetchCount(10); // start low, increase after load testing

        // Concurrency: min/max threads per listener
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        factory.setMessageConverter(jackson2JsonMessageConverter());
        factory.setAfterReceivePostProcessors(correlationReceivePostProcessor);
        factory.setAdviceChain(correlationAdvice, retryInterceptor(amqpTemplate));

        return factory;
    }

    @Bean
    public RetryOperationsInterceptor retryInterceptor(AmqpTemplate amqpTemplate) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000) // initial interval, multiplier, max interval
                .recoverer(new RejectAndDontRequeueRecoverer()) // will route to DLX via x-dead-letter-exchange
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public MessageConverter messageConverter() {
        return jackson2JsonMessageConverter();
    }
}
