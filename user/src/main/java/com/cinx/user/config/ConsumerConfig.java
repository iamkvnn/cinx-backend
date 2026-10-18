package com.cinx.user.config;

import com.cinx.common.logging.RabbitCorrelationAdvice;
import com.cinx.common.logging.RabbitCorrelationReceivePostProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ConsumerConfig {
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RabbitCorrelationReceivePostProcessor correlationReceivePostProcessor,
            RabbitCorrelationAdvice correlationAdvice,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAutoStartup(autoStartup);

        // Manual ack — never lose a message silently
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // Tune prefetch to control how many unacknowledged messages a consumer holds
        factory.setPrefetchCount(10); // start low, increase after load testing

        // Concurrency: min/max threads per listener
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // Retry with backoff on listener errors
        factory.setDefaultRequeueRejected(false); // send failed messages to DLX
        factory.setMessageConverter(jackson2JsonMessageConverter);
        factory.setAfterReceivePostProcessors(correlationReceivePostProcessor);
        factory.setAdviceChain(correlationAdvice);

        return factory;
    }
}
