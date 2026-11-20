package com.cinx.commerce.subscription.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "current_subject_subscription")
@IdClass(CurrentSubscriptionPointerJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class CurrentSubscriptionPointerJpaEntity {
    @Id
    @Column(name = "subject_id", nullable = false)
    private String subjectId;
    @Id
    @Column(name = "subscription_slot_code", nullable = false)
    private String subscriptionSlotCode;
    @Column(name = "subscription_id", nullable = false)
    private String subscriptionId;
    private String activeCommandId;
    private LocalDateTime activeCommandStartedAt;
    @Version
    @Column(nullable = false)
    private long optimisticVersion;

    public record Key(String subjectId, String subscriptionSlotCode) implements Serializable {
    }
}
