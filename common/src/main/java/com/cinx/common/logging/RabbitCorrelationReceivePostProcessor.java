package com.cinx.common.logging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

public class RabbitCorrelationReceivePostProcessor implements MessagePostProcessor {
    @Override
    public Message postProcessMessage(Message message) {
        CorrelationContext.TraceHeaders headers =
                CorrelationContext.fromMessageHeaders(message.getMessageProperties().getHeaders());
        CorrelationContext.put(headers);
        return message;
    }
}
