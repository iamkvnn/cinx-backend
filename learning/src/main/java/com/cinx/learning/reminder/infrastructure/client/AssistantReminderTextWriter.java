package com.cinx.learning.reminder.infrastructure.client;

import com.cinx.learning.reminder.application.ReminderMessage;
import com.cinx.learning.reminder.application.ReminderTextWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Any failure (switched off, timeout, a rejected rewrite, no token) leaves the template in place. */
@Slf4j
@Component
@RequiredArgsConstructor
class AssistantReminderTextWriter implements ReminderTextWriter {
    private final AssistantReminderClient client;

    @Override
    public Optional<Text> rewrite(ReminderMessage template) {
        try {
            AssistantReminderClient.TextResponse text = client.write(new AssistantReminderClient.TextRequest(
                    template.reason().name(), template.facts(),
                    new AssistantReminderClient.Template(template.title(), template.body())));
            if (text == null || isBlank(text.title()) || isBlank(text.body())) {
                return Optional.empty();
            }
            return Optional.of(new Text(text.title(), text.body()));
        } catch (RuntimeException e) {
            log.info("Study reminder keeps its template ({}): {}", template.reason(), e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
