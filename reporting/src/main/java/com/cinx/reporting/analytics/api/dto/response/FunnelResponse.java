package com.cinx.reporting.analytics.api.dto.response;

import java.time.LocalDate;
import java.util.List;

public record FunnelResponse(LocalDate from, LocalDate to, List<Step> steps) {
    public record Step(String name, long users) {
    }
}
