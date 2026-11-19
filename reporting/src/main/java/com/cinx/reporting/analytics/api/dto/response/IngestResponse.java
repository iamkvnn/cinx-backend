package com.cinx.reporting.analytics.api.dto.response;

public record IngestResponse(int accepted, int duplicates, int dropped) {
}
