package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.shared.domain.DailyGoalType;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity(name = "UserDailyGoal")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "goal_date", "goal_key"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyGoalJpaEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyGoalType goalType;

    @Column(name = "goal_key", nullable = false)
    private String goalKey;

    @Column(nullable = false)
    private Integer targetValue;
    
    @lombok.Builder.Default
    @Column(nullable = false)
    private Integer currentValue = 0;

    private String targetItemId;
    
    @Column(name = "goal_date", nullable = false)
    private LocalDate goalDate;
    
    @lombok.Builder.Default
    @Column(nullable = false)
    private Boolean isCompleted = false;
}
