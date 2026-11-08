package com.cinx.commerce.subscription.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_subject", uniqueConstraints = @UniqueConstraint(
        name = "uk_subscription_subject", columnNames = {"subject_type", "subject_reference"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionSubjectJpaEntity {
    @Id private String id;
    @Column(name = "subject_type", nullable = false) private String subjectType;
    @Column(name = "subject_reference", nullable = false) private String subjectReference;
    @Column(nullable = false) private LocalDateTime createdAt;
}
