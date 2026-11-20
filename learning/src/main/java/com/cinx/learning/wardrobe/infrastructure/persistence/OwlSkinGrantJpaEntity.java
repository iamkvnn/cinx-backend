package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "OwlSkinGrant")
@Table(name = "owl_skin_grant", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skin_code"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OwlSkinGrantJpaEntity extends BaseEntity {
    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 40)
    private String skinCode;

    /** UTC. */
    @Column(nullable = false)
    private LocalDateTime grantedAt;
}
