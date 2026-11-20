package com.cinx.notification.notification.infrastructure.messaging.event.campus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * campus.event.scheduled, campus.event.reminder.due, campus.event.cancelled and campus.thread.replied v1 share this
 * shape; the routing key says which. {@code eventId} is the campus event's id: the message's own id is the AMQP message id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusEventNotice {
    private String eventId;
    private String zoneId;
    private String zoneTitle;
    /** Scheduled only. */
    private String instructorId;
    private String title;
    /** quiz, lecture or meetup; absent on a cancellation. */
    private String kind;
    private Instant startsAt;
    /** Scheduled only. */
    private Instant endsAt;
    /** Thread replied only, like the six fields after it; {@code title} is then the thread's title. */
    private String threadId;
    private String replyId;
    /** zone or club. */
    private String placeKind;
    private String placeId;
    /** The zone's or club's title; may be absent. */
    private String placeTitle;
    private String replierId;
    private String replierName;
    private List<String> recipientIds;
    private Instant occurredAt;
}
