package com.cinx.learning.reminder.application;

import java.util.Optional;

/** Port: the assistant's rewrite of a template. Empty whenever it cannot help (off, slow, failed): the template goes. */
public interface ReminderTextWriter {
    Optional<Text> rewrite(ReminderMessage template);

    record Text(String title, String body) {
    }
}
