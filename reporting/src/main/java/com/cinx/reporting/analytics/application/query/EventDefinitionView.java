package com.cinx.reporting.analytics.application.query;

import java.util.List;

/** An event of the dictionary, for the admin's step and goal pickers. */
public record EventDefinitionView(String name, List<Prop> props) {
    public record Prop(String name, String type, boolean required, List<String> values) {
    }
}
