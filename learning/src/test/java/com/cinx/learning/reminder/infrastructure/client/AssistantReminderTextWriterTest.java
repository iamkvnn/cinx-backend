package com.cinx.learning.reminder.infrastructure.client;

import com.cinx.learning.reminder.application.ReminderMessage;
import com.cinx.learning.reminder.application.ReminderTextWriter;
import com.cinx.learning.reminder.domain.ReminderReason;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantReminderTextWriterTest {
    private static final ReminderMessage TEMPLATE = new ReminderMessage(ReminderReason.STREAK_AT_RISK, "Giữ chuỗi 4 ngày nhé",
            "Bạn đã học 4 ngày liền.", "/week", Map.of("streakDays", 4));
    private static final Request REQUEST = Request.create(Request.HttpMethod.POST, "http://assistant:8092/internal/reminders/text",
            Map.of(), new byte[0], StandardCharsets.UTF_8, null);

    @Mock
    private AssistantReminderClient client;

    @InjectMocks
    private AssistantReminderTextWriter writer;

    @Test
    void theAssistantsWordingIsUsed() {
        when(client.write(any())).thenReturn(new AssistantReminderClient.TextResponse("Chuỗi 4 ngày!", "Học một bài nhé."));

        assertThat(writer.rewrite(TEMPLATE)).contains(new ReminderTextWriter.Text("Chuỗi 4 ngày!", "Học một bài nhé."));
    }

    @Test
    void aSwitchedOffOrRejectingOrSlowAssistantLeavesTheTemplate() {
        for (RuntimeException failure : new RuntimeException[]{
                new FeignException.ServiceUnavailable("CAPABILITY_DISABLED", REQUEST, null, null),
                new FeignException.UnprocessableEntity("REMINDER_REJECTED", REQUEST, null, null),
                new FeignException.Forbidden("FORBIDDEN", REQUEST, null, null),
                new RetryableException(-1, "Read timed out", Request.HttpMethod.POST, (Long) null, REQUEST)}) {
            doThrow(failure).when(client).write(any());
            assertThat(writer.rewrite(TEMPLATE)).isEqualTo(Optional.empty());
        }
    }

    @Test
    void aBlankAnswerLeavesTheTemplate() {
        when(client.write(any())).thenReturn(new AssistantReminderClient.TextResponse(" ", "Học một bài nhé."));

        assertThat(writer.rewrite(TEMPLATE)).isEmpty();
    }
}
