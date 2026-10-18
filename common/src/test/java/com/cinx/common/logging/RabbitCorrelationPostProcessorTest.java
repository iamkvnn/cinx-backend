package com.cinx.common.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitCorrelationPostProcessorTest {

    @Test
    void copiesCurrentCorrelationToPublishedMessageHeaders() {
        CorrelationContext.TraceHeaders headers = CorrelationContext.fromHeaders(null, "request-456");
        CorrelationContext.put(headers);
        try {
            Message message = new Message(new byte[0], new MessageProperties());

            new RabbitCorrelationPostProcessor().postProcessMessage(message);

            assertThat(message.getMessageProperties().getHeader(CorrelationContext.TRACEPARENT_HEADER).toString())
                    .startsWith("00-" + headers.traceId() + "-")
                    .endsWith("-01");
            assertThat((String) message.getMessageProperties().getHeader(CorrelationContext.REQUEST_ID_HEADER))
                    .isEqualTo("request-456");
        } finally {
            CorrelationContext.clear();
        }
    }
}
