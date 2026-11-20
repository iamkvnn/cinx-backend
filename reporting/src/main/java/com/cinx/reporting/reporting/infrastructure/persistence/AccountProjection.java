package com.cinx.reporting.reporting.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_projection")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountProjection {
    @Id
    private String userId;
    private String status;
    private String role;
    @Column(name = "roles_json")
    private String rolesJson;
    @Column(name = "changed_at")
    private LocalDateTime changedAt;
    private LocalDateTime updatedAt;
}
