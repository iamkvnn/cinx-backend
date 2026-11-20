package com.cinx.reporting.analytics.api.dto.response;

import java.util.List;

public record ExperimentResultsResponse(ExperimentResponse experiment, List<VariantResult> variants) {
    public record VariantResult(String variant, long exposed, long converted) {
    }
}
