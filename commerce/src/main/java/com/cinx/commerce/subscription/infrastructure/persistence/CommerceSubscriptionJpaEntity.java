package com.cinx.commerce.subscription.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_subscription")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommerceSubscriptionJpaEntity {
    @Id private String id;
    @Column(nullable = false) private String subjectId;
    @Column(nullable = false) private String planVersionId;
    private String priceId;
    private String assignmentBindingId;
    @Column(nullable = false) private String origin;
    private String provider;
    private String providerAccountKey;
    private String providerCustomerRef;
    private String providerSubscriptionRef;
    @Column(nullable = false) private String billingStatus;
    @Column(nullable = false) private String accessStatus;
    @Column(nullable = false) private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialStartedAt;
    private LocalDateTime trialEndsAt;
    private LocalDateTime graceStartedAt;
    private LocalDateTime graceEndsAt;
    @Column(nullable = false) private boolean cancelAtPeriodEnd;
    private LocalDateTime canceledAt;
    private LocalDateTime endedAt;
    @Column(name = "provider_object_version", nullable = false)
    private long providerObjectVersion;
    @Column(nullable = false) private long aggregateVersion;
    @Version @Column(nullable = false) private long optimisticVersion;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
