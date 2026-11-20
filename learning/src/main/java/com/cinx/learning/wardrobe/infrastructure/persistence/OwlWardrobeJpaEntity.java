package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "OwlWardrobe")
@Table(name = "owl_wardrobe")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OwlWardrobeJpaEntity extends AuditableEntity {
    @Column(nullable = false, unique = true)
    private String userId;

    @Column(length = 40)
    private String palette;

    @Column(length = 40)
    private String head;

    @Column(length = 40)
    private String face;

    @Column(length = 40)
    private String effect;

    @Column(length = 40)
    private String hold;
}
