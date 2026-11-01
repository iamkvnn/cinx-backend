package com.cinx.learning.reminder.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The reminder hour is Vietnam time; quiet hours (21:00–08:00) are outside the range. */
public record StudyReminderRequest(@NotNull Boolean enabled, @NotNull @Min(8) @Max(20) Integer hour) {
}
