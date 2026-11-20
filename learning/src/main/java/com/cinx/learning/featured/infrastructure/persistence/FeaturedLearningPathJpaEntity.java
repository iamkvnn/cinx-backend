package com.cinx.learning.featured.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/** An admin-curated learning path shown publicly ("Lộ trình nổi bật"): an ordered list of published courses. */
@Entity(name = "FeaturedLearningPath")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedLearningPathJpaEntity extends AuditableEntity {
    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String roleTitle;

    @Column(nullable = false)
    private Integer estimatedMonths;

    @Column(nullable = false)
    private Integer hoursPerWeek;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "featured_learning_path_course", joinColumns = @JoinColumn(name = "path_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "course_id", nullable = false)
    private List<String> courseIds = new ArrayList<>();

    @Column(length = 500)
    private String description;

    /** "After this path you can …" lines on the public path page; lazy, read only by the detail and admin views. */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "featured_learning_path_outcome", joinColumns = @JoinColumn(name = "path_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "outcome", length = 120, nullable = false)
    @BatchSize(size = 50)
    private List<String> outcomes = new ArrayList<>();
}
