package com.cinx.reporting.analytics.application.command;

import java.util.List;
import java.util.Map;

public record CreateExperimentCommand(String key, String description, List<VariantWeight> variants, String goalEvent,
                                      Map<String, String> goalFilter, int goalWindowDays) {
    public record VariantWeight(String key, int weight) {
    }
}
