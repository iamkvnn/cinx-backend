package com.cinx.commerce.catalog.api.mapper;

import com.cinx.commerce.catalog.api.dto.PlanDraftRequest;
import com.cinx.commerce.catalog.api.dto.PublishedPlanResponse;
import com.cinx.commerce.catalog.application.command.CreatePlanDraftCommand;
import com.cinx.commerce.catalog.application.query.PublishedPlanView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanCatalogMapper {
    PublishedPlanResponse toResponse(PublishedPlanView view);

    CreatePlanDraftCommand toCommand(PlanDraftRequest request);
}
