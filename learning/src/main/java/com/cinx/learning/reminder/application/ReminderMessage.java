package com.cinx.learning.reminder.application;

import com.cinx.learning.reminder.domain.ReminderReason;

import java.util.Map;

/**
 * One reminder: why, the text, where it opens, and the facts the text was written from. The facts are what the
 * assistant may rewrite from; it may not state anything they do not say.
 */
public record ReminderMessage(ReminderReason reason, String title, String body, String actionUrl, Map<String, Object> facts) {
    public ReminderMessage withText(String newTitle, String newBody) {
        return new ReminderMessage(reason, newTitle, newBody, actionUrl, facts);
    }
}
