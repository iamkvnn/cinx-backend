package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.common.model.AuditableEntity;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "policy_documents",
        indexes = {
                @Index(name = "idx_policy_slug_status", columnList = "slug,status"),
                @Index(name = "idx_policy_type_status", columnList = "policy_type,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDocumentJpaEntity extends AuditableEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private PolicyType policyType;
    @Column(nullable = false, length = 120)
    private String slug;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(length = 1000)
    private String summary;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyStatus status;
    @Column(nullable = false)
    private Integer versionNumber;
    private LocalDateTime effectiveAt;
    private LocalDateTime publishedAt;
    private Integer displayOrder;
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<PolicySectionJpaEntity> sections = new ArrayList<>();
}
