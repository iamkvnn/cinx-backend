package com.cinx.auth.identity.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "authorization_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationAudit {
    @Id private String id;
    @Column(name = "actor_type", nullable = false) private String actorType;
    @Column(name = "actor_id", nullable = false) private String actorId;
    @Column(name = "target_user_id", nullable = false) private String targetUserId;
    @Column(nullable = false) private String action;
    @Column(name = "role_code") private String roleCode;
    @Column(name = "before_json") private String beforeJson;
    @Column(name = "after_json", nullable = false) private String afterJson;
    @Column(name = "authorization_policy_version", nullable = false) private long authorizationPolicyVersion;
    @Column(name = "request_id") private String requestId;
    @Column(name = "trace_id") private String traceId;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
