package com.cinx.commerce.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The display copy of one plan version in one locale. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanVersionTranslation {
    private String planVersionId;
    private String locale;
    private String displayName;
    private String description;
    private String badge;
}
