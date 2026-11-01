package com.cinx.learning.engagement.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "UserExperience")
@Table(name = "user_experience")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExperienceJpaEntity {
    @Id
    @Column(name = "user_id")
    private String userId;
    @Column(nullable = false)
    private Integer xp;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
