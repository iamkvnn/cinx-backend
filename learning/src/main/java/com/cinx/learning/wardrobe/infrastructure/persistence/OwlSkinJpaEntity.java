package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.common.model.AuditableEntity;
import com.cinx.learning.wardrobe.domain.OwlSkinSlot;
import com.cinx.learning.wardrobe.domain.OwlSkinUnlockRule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity(name = "OwlSkin")
@Table(name = "owl_skin")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OwlSkinJpaEntity extends AuditableEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OwlSkinSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OwlSkinUnlockRule unlockRule;

    private Integer streakDays;

    private LocalDate seasonStartsOn;

    private LocalDate seasonEndsOn;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
