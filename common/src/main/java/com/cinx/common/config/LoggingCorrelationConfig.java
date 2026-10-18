package com.cinx.common.config;

import com.cinx.common.logging.RabbitCorrelationAdvice;
import com.cinx.common.logging.RabbitCorrelationPostProcessor;
import com.cinx.common.logging.RabbitCorrelationReceivePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingCorrelationConfig {

    @Bean
    public RabbitCorrelationPostProcessor rabbitCorrelationPostProcessor() {
        return new RabbitCorrelationPostProcessor();
    }

    @Bean
    public RabbitCorrelationAdvice rabbitCorrelationAdvice() {
        return new RabbitCorrelationAdvice();
    }

    @Bean
    public RabbitCorrelationReceivePostProcessor rabbitCorrelationReceivePostProcessor() {
        return new RabbitCorrelationReceivePostProcessor();
    }

    @Bean
    public BeanPostProcessor rabbitLoggingCorrelationPostProcessor(RabbitCorrelationPostProcessor postProcessor) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RabbitTemplate rabbitTemplate) {
                    rabbitTemplate.addBeforePublishPostProcessors(postProcessor);
                }
                return bean;
            }
        };
    }
}
