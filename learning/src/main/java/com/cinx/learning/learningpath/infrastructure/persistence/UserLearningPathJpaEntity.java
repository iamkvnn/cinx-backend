package com.cinx.learning.learningpath.infrastructure.persistence;

import com.cinx.learning.shared.domain.LearningPathStatus;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "UserLearningPath")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserLearningPathJpaEntity extends BaseEntity {
    private String userId;
    private String title;
    @Column(length = 2000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    private LearningPathStatus status;
    
    private Double currentProgress;
    private Integer totalItems;
    private Integer completedItems;

    // Set when the create request carried an Idempotency-Key.
    private String idempotencyKey;
    @Column(length = 64)
    private String requestFingerprint;

    // UTC, from the learning clock. Paths written before V20 have none.
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime droppedAt;

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LearningPathItemJpaEntity> items = new ArrayList<>();
}
