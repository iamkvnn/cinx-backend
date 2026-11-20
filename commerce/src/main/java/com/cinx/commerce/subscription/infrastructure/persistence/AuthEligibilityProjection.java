package com.cinx.commerce.subscription.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_identity_eligibility")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthEligibilityProjection {
    @Id @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "account_status", nullable = false) private String accountStatus;
    @Lob @Column(name = "roles_json", nullable = false, columnDefinition = "JSON") private String rolesJson;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
