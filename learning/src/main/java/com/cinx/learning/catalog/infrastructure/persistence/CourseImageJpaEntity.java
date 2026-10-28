package com.cinx.learning.catalog.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity(name = "CourseImage")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseImageJpaEntity extends BaseEntity {
    private String imageUrl;
    private String publicId;

    /** Position in the course's gallery; the first image is the cover. */
    @Builder.Default
    @Column(nullable = false)
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    private CourseJpaEntity course;
}

