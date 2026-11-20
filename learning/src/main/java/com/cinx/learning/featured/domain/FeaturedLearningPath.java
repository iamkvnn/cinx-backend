package com.cinx.learning.featured.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.util.ArrayList;
import java.util.List;

/** An admin-curated learning path shown publicly ("Lộ trình nổi bật"): an ordered list of published courses. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedLearningPath extends AuditedModel {
    private String name;
    private String roleTitle;
    private Integer estimatedMonths;
    private Integer hoursPerWeek;
    private Integer sortOrder;
    @Builder.Default
    private Boolean active = true;
    @Builder.Default
    private List<String> courseIds = new ArrayList<>();
    private String description;
    /** "After this path you can …" lines on the public path page; lazy, read only by the detail and admin views. */
    @Builder.Default
    private List<String> outcomes = new ArrayList<>();
}
