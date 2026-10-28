package com.cinx.learning.catalog.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "RejectCourseReason")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RejectCourseReasonJpaEntity extends AuditableEntity {
    private String courseId;

    @Column(columnDefinition = "TEXT")
    private String reason;
}

