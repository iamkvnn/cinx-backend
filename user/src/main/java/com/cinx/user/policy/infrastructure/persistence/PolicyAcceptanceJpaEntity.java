package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "policy_acceptance",
        indexes = @Index(name = "idx_policy_acceptance_policy", columnList = "policy_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAcceptanceJpaEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "policy_id", nullable = false)
    private String policyId;
    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;
    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;
}
