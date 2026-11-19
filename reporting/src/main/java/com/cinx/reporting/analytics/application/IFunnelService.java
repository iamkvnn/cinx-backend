package com.cinx.reporting.analytics.application;

import com.cinx.reporting.analytics.application.query.EventDefinitionView;
import com.cinx.reporting.analytics.application.query.FunnelView;

import java.time.LocalDate;
import java.util.List;

public interface IFunnelService {
    int MIN_STEPS = 2;
    int MAX_STEPS = 5;

    /** Users through 2-5 ordered steps between two Vietnam days, both included, inside the raw events' 180 days. */
    FunnelView funnel(List<String> steps, LocalDate from, LocalDate to);

    List<EventDefinitionView> catalog();
}
