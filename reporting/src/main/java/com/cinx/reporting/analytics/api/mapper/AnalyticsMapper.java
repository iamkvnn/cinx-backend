package com.cinx.reporting.analytics.api.mapper;

import com.cinx.reporting.analytics.api.dto.request.CreateExperimentRequest;
import com.cinx.reporting.analytics.api.dto.request.ProductEventRequest;
import com.cinx.reporting.analytics.api.dto.response.AssignmentResponse;
import com.cinx.reporting.analytics.api.dto.response.EventDefinitionResponse;
import com.cinx.reporting.analytics.api.dto.response.ExperimentResponse;
import com.cinx.reporting.analytics.api.dto.response.ExperimentResultsResponse;
import com.cinx.reporting.analytics.api.dto.response.FunnelResponse;
import com.cinx.reporting.analytics.api.dto.response.IngestResponse;
import com.cinx.reporting.analytics.application.command.CreateExperimentCommand;
import com.cinx.reporting.analytics.application.command.IncomingEvent;
import com.cinx.reporting.analytics.application.query.AssignmentView;
import com.cinx.reporting.analytics.application.query.EventDefinitionView;
import com.cinx.reporting.analytics.application.query.ExperimentResultsView;
import com.cinx.reporting.analytics.application.query.ExperimentView;
import com.cinx.reporting.analytics.application.query.FunnelView;
import com.cinx.reporting.analytics.application.query.IngestResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AnalyticsMapper {
    IncomingEvent toCommand(ProductEventRequest request);

    List<IncomingEvent> toCommands(List<ProductEventRequest> requests);

    CreateExperimentCommand toCommand(CreateExperimentRequest request);

    IngestResponse toDto(IngestResult result);

    List<AssignmentResponse> toAssignments(List<AssignmentView> views);

    ExperimentResponse toDto(ExperimentView view);

    List<ExperimentResponse> toExperiments(List<ExperimentView> views);

    ExperimentResultsResponse toDto(ExperimentResultsView view);

    FunnelResponse toDto(FunnelView view);

    List<EventDefinitionResponse> toCatalog(List<EventDefinitionView> views);
}
