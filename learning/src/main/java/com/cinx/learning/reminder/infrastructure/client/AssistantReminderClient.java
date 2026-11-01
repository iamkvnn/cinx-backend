package com.cinx.learning.reminder.infrastructure.client;

import com.cinx.learning.config.AssistantFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** The assistant is not in Eureka: it is reached at a configured address, with a 5-second read timeout. */
@FeignClient(name = "assistant-reminders", url = "${cinx.assistant.internal-base-url:http://assistant:8092}",
        path = "/internal/reminders", configuration = AssistantFeignConfig.class)
interface AssistantReminderClient {
    @PostMapping("/text")
    TextResponse write(@RequestBody TextRequest request);

    record TextRequest(String reason, Map<String, Object> facts, Template template) {
    }

    record Template(String title, String body) {
    }

    record TextResponse(String title, String body) {
    }
}
