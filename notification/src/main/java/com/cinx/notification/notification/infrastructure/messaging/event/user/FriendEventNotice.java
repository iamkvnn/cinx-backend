package com.cinx.notification.notification.infrastructure.messaging.event.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * user.friend.requested and user.friend.accepted v1 share this shape; the routing key says which and which fields are
 * set. {@code requestId} is the friend request's id: the message's own id is the AMQP message id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendEventNotice {
    private String requestId;
    /** Requested only. */
    private String fromUserId;
    private String fromName;
    private String toUserId;
    /** Accepted only. */
    private String accepterId;
    private String accepterName;
    private String requesterId;
    private Instant occurredAt;
}
