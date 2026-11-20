package com.cinx.reporting.analytics.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.reporting.analytics.api.dto.request.ProductEventBatchRequest;
import com.cinx.reporting.analytics.api.dto.response.IngestResponse;
import com.cinx.reporting.analytics.api.mapper.AnalyticsMapper;
import com.cinx.reporting.analytics.application.IProductEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Product events of the signed-in user (P0-07); best effort, so the client never retries more than once. */
@RestController
@RequestMapping("/api/v1/reporting/events")
@RequiredArgsConstructor
public class ProductEventController {
    private final IProductEventService productEvents;
    private final AnalyticsMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Record my product events", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<IngestResponse> record(@Valid @RequestBody ProductEventBatchRequest request) {
        return new ApiResponse<>(true, "Product events recorded",
                mapper.toDto(productEvents.ingest(AuthenticationUtil.extractUserId(), mapper.toCommands(request.events()))));
    }
}
