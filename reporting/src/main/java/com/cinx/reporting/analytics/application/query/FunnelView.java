package com.cinx.reporting.analytics.application.query;

import java.time.LocalDate;
import java.util.List;

/** Users who did each step, in order, between {@code from} and {@code to} (Vietnam days, both included). */
public record FunnelView(LocalDate from, LocalDate to, List<Step> steps) {
    public record Step(String name, long users) {
    }
}
