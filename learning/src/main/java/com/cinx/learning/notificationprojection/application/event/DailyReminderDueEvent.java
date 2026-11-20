package com.cinx.learning.notificationprojection.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReminderDueEvent {
    private String userId;
    private String goalType;
    private int targetValue;
    private int currentValue;
    private String targetItemId;
    private Instant occurredAt;
}
