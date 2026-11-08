package com.cinx.commerce.subscription.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Which subscription is the subject's current one in a slot. It is locked while that subscription is replaced. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSubscriptionPointer {
    private String subjectId;
    private String subscriptionSlotCode;
    private String subscriptionId;
    private String activeCommandId;
    private LocalDateTime activeCommandStartedAt;
    /** The persistence version (optimistic lock); the store keeps it. */
    private long optimisticVersion;
}
