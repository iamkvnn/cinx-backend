package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseAccessModel;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity(name = "Course")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseJpaEntity extends AuditableEntity {
    private String title;
    @Column(length = 2000)
    private String description;
    private Long duration;
    @Column(precision = 19, scale = 2)
    private BigDecimal price;
    @Column(precision = 19, scale = 2)
    private BigDecimal discountedPrice;
    private Long discountRate;
    private Double rating;
    @Builder.Default
    @Column(nullable = false)
    private Long ratingCount = 0L;
    private Long enrollmentCount;
    private Boolean isInSubscription;
    @Enumerated(EnumType.STRING)
    private CourseAccessModel accessModel;
    private String subscriptionFeatureKey;
    private String instructorId;

    @Builder.Default
    private Boolean hasCertificate = false;
    private String certificateTitle;
    @Builder.Default
    @Column(nullable = false)
    private Boolean aiAssisted = false;
    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @Enumerated(EnumType.STRING)
    private CoursePublishStatus publishStatus;

    /** First approval; never cleared, so a course that was ever on sale cannot be hard-deleted. */
    private LocalDateTime publishedAt;
    /** The pending review request; cleared on approve, reject and withdraw. */
    private LocalDateTime submittedAt;
    private LocalDateTime archivedAt;

    /** Intro video; lives on the course like the images, so a change is live at once. Duration in seconds. */
    @Column(length = 512)
    private String promoVideoKey;
    @Column(length = 1024)
    private String promoVideoUrl;
    private String promoVideoFileName;
    private Long promoVideoFileSize;
    private Integer promoVideoDuration;

    @ManyToOne
    private CategoryJpaEntity category;

    @Builder.Default
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CourseImageJpaEntity> images = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "course_outcome", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "outcome", length = 120, nullable = false)
    @BatchSize(size = 50)
    private List<String> outcomes = new ArrayList<>();
}

