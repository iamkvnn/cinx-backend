package com.cinx.learning.entitlement.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_user_feature_entitlement")
@IdClass(UserFeatureEntitlement.Key.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserFeatureEntitlement {
    @Id @Column(nullable = false) private String userId;
    @Id @Column(nullable = false) private String featureKey;
    @Column(nullable = false) private String valueType;
    private Boolean enabled;
    private Long quotaLimit;
    private Boolean quotaUnlimited;
    private String quotaUnit;
    private String resetPolicy;
    private LocalDateTime quotaPeriodStart;
    private LocalDateTime quotaPeriodEnd;
    private LocalDateTime effectiveUntil;
    @Column(nullable = false) private LocalDateTime updatedAt;

    public record Key(String userId, String featureKey) implements Serializable {
    }
}
