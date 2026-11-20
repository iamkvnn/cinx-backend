package com.cinx.reporting.analytics.application;

import com.cinx.reporting.analytics.application.query.ExperimentResultsView.VariantResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Read-side counts over the raw events; implemented in SQL by the infrastructure. */
public interface AnalyticsQueries {
    /** Exposed and converted users per variant that has any exposure. */
    List<VariantResult> experimentResults(String experimentKey, String goalEvent, Map<String, String> goalFilter, int windowDays);

    /** Users at each step: a step counts only after the same user's first previous step, inside the UTC range [from, to). */
    List<Long> funnel(List<String> steps, LocalDateTime from, LocalDateTime to);
}
