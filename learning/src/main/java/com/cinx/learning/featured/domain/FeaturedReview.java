package com.cinx.learning.featured.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

/** A course review picked for the home page ("Học viên nói gì"). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedReview extends AuditedModel {
    private String reviewId;
    private Integer sortOrder;
    @Builder.Default
    private Boolean active = true;
}
