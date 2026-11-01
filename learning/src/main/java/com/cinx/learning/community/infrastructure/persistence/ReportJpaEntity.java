package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.ReportType;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "Report")
@Table(
    name = "reports",
    indexes = {
        @Index(name = "idx_report_ref_type", columnList = "refId, type")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String reporterId;

    @Column(nullable = false)
    private String refId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;
}
