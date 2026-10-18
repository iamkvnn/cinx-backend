package com.cinx.common.logging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

public class RabbitCorrelationPostProcessor implements MessagePostProcessor {
    @Override
    public Message postProcessMessage(Message message) {
        CorrelationContext.applyMissingToMessageProperties(message.getMessageProperties());
        return message;
    }
}
