package com.cinx.reporting.analytics.application.query;

import java.util.List;

/** Per variant: learners exposed, and how many of them did the goal within the window after their exposure. */
public record ExperimentResultsView(ExperimentView experiment, List<VariantResult> variants) {
    public record VariantResult(String variant, long exposed, long converted) {
    }
}
