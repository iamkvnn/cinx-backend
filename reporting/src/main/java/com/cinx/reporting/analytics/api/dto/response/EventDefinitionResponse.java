package com.cinx.reporting.analytics.api.dto.response;

import java.util.List;

public record EventDefinitionResponse(String name, List<Prop> props) {
    public record Prop(String name, String type, boolean required, List<String> values) {
    }
}
