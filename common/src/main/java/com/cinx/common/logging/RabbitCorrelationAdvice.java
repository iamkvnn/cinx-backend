package com.cinx.common.logging;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.Message;

public class RabbitCorrelationAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Message message = findMessage(invocation.getArguments());
        if (message != null) {
            CorrelationContext.TraceHeaders headers =
                    CorrelationContext.fromMessageHeaders(message.getMessageProperties().getHeaders());
            CorrelationContext.put(headers);
        }
        try {
            return invocation.proceed();
        } finally {
            CorrelationContext.clear();
        }
    }

    private Message findMessage(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument instanceof Message message) {
                return message;
            }
        }
        return null;
    }
}
