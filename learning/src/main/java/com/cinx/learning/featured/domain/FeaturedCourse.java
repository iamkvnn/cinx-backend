package com.cinx.learning.featured.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

/** A course picked for the home page carousel ("Khoá nổi bật tháng này"). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedCourse extends AuditedModel {
    private String courseId;
    private Integer sortOrder;
    @Builder.Default
    private Boolean active = true;
}
