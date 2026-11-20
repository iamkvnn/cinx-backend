package com.cinx.learning.catalog.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "Section")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SectionJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String stableId;

    private String title;
    private String description;
    private Long duration;
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    private CourseJpaEntity course; // Published course

    @ManyToOne(fetch = FetchType.LAZY)
    private CourseDraftJpaEntity draft; // Draft course

    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonJpaEntity> lessons = new ArrayList<>();
}

