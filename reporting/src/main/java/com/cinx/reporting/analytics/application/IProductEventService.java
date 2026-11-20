package com.cinx.reporting.analytics.application;

import com.cinx.reporting.analytics.application.command.IncomingEvent;
import com.cinx.reporting.analytics.application.query.IngestResult;

import java.util.List;

public interface IProductEventService {
    int MAX_BATCH = 50;

    /** Stores a batch of the user's events; an event that breaks the dictionary refuses the whole batch. */
    IngestResult ingest(String userId, List<IncomingEvent> batch);
}
