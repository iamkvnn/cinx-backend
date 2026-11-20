package com.cinx.learning.catalog.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "CourseDraft")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDraftJpaEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private CourseJpaEntity course;

    private String title;
    @Column(length = 2000)
    private String description;
    private Long duration;
    @Column(precision = 19, scale = 2)
    private BigDecimal price;
    @Column(precision = 19, scale = 2)
    private BigDecimal discountedPrice;
    private Long discountRate;
    private Boolean isInSubscription;
    private Boolean hasCertificate;
    private String certificateTitle;
    @Builder.Default
    @Column(nullable = false)
    private Boolean aiAssisted = false;

    @ManyToOne
    private CategoryJpaEntity category;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "course_draft_outcome", joinColumns = @JoinColumn(name = "course_draft_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "outcome", length = 120, nullable = false)
    private List<String> outcomes = new ArrayList<>();
}

