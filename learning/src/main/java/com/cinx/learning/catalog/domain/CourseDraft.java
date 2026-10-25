package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDraft extends AuditedModel {
    private String courseId;
    private String title;
    private String description;
    private Long duration;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Long discountRate;
    private Boolean isInSubscription;
    private Boolean hasCertificate;
    private String certificateTitle;
    /** Copied to the course on approval; only ever turned on. */
    @Builder.Default
    private Boolean aiAssisted = false;
    private String categoryId;
    @Builder.Default
    private List<String> outcomes = new ArrayList<>();
}
