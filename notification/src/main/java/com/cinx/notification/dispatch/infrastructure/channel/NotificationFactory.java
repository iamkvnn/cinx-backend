package com.cinx.notification.dispatch.infrastructure.channel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationFactory {

    private final Map<String, NotificationChannelStrategy> strategies;

    public NotificationFactory(List<NotificationChannelStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(NotificationChannelStrategy::getChannelName, Function.identity()));
    }

    public NotificationChannelStrategy getStrategy(String channelName) {
        NotificationChannelStrategy strategy = strategies.get(channelName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported notification channel: " + channelName);
        }
        return strategy;
    }
}
