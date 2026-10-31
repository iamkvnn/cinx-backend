package com.cinx.learning.assessment.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class UserSubmissionJpaEntity extends BaseEntity {
    private String userId;
    private LocalDateTime submissionTime;
}
