package com.cinx.learning.featured.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A course picked for the home page carousel ("Khoá nổi bật tháng này"). */
@Entity(name = "FeaturedCourse")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedCourseJpaEntity extends AuditableEntity {
    @Column(nullable = false, unique = true)
    private String courseId;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
