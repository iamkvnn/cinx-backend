package com.cinx.notification.notification.application.query;

import java.util.Map;

/** A user's inbox in numbers: everything, the unread part, and the count per type. */
public record NotificationCounts(long total, long unread, Map<String, Long> byType) {
}
