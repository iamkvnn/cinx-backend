package com.cinx.commerce.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "plan_version_translation")
@IdClass(PlanVersionTranslationJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class PlanVersionTranslationJpaEntity {
    @Id
    @Column(name = "plan_version_id")
    private String planVersionId;
    @Id
    @Column(name = "locale")
    private String locale;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    private String description;
    private String badge;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String planVersionId;
        private String locale;
    }
}
