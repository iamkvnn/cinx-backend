package com.cinx.commerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Rejected messages are not requeued, so every consumed queue must dead-letter into a declared DLQ or they are lost. */
class RabbitMQConfigTest {

    @Test
    void everyConsumedQueueDeadLettersIntoABoundQueue() {
        try (var context = new AnnotationConfigApplicationContext(RabbitMQConfig.class)) {
            Map<String, Queue> queues = context.getBeansOfType(Queue.class).values().stream()
                    .collect(Collectors.toMap(Queue::getName, queue -> queue));
            Set<String> consumed = listenerQueues().stream()
                    .map(name -> context.getEnvironment().resolvePlaceholders(name))
                    .collect(Collectors.toSet());

            assertThat(consumed).isNotEmpty();
            for (String name : consumed) {
                assertThat(queues).containsKey(name);
                Map<String, Object> arguments = queues.get(name).getArguments();
                assertThat(arguments).as(name).containsEntry("x-dead-letter-exchange", "cinx.dlx");
                Object routingKey = arguments.get("x-dead-letter-routing-key");
                assertThat(context.getBeansOfType(Binding.class).values()).as(name)
                        .anySatisfy(binding -> {
                            assertThat(binding.getExchange()).isEqualTo("cinx.dlx");
                            assertThat(binding.getRoutingKey()).isEqualTo(routingKey);
                            assertThat(queues).containsKey(binding.getDestination());
                        });
            }
        }
    }

    @Test
    void failedMessagesAreRetriedThenRejectedWithoutRequeue() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        Properties properties = yaml.getObject();

        assertThat(properties.getProperty("spring.rabbitmq.listener.simple.default-requeue-rejected")).isEqualTo("false");
        assertThat(properties.getProperty("spring.rabbitmq.listener.simple.retry.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("spring.rabbitmq.listener.simple.retry.max-attempts")).isEqualTo("5");
    }

    private static Set<String> listenerQueues() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        return scanner.findCandidateComponents("com.cinx.commerce").stream()
                .map(BeanDefinition::getBeanClassName)
                .map(name -> ClassUtils.resolveClassName(name, RabbitMQConfigTest.class.getClassLoader()))
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(RabbitMQConfigTest::listener)
                .filter(listener -> listener != null)
                .flatMap(listener -> Arrays.stream(listener.queues()))
                .collect(Collectors.toSet());
    }

    private static RabbitListener listener(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, RabbitListener.class);
    }
}
