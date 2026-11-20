package com.cinx.learning.dailyquiz.api;

import com.cinx.learning.featured.api.mapper.FeaturedApiMapper;

import com.cinx.learning.dailyquiz.api.mapper.DailyquizApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.dailyquiz.api.dto.DailyQuizResponse;
import com.cinx.learning.dailyquiz.api.dto.UpsertDailyQuizRequest;
import com.cinx.learning.dailyquiz.application.IDailyQuizService;
import com.cinx.learning.featured.api.dto.request.ReorderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin management of the daily quiz pool. Reads need the permission; writes also need MFA. */
@RestController
@RequestMapping("/api/v1/learning/admin/daily-quiz")
@RequiredArgsConstructor
public class AdminDailyQuizController {
    private static final String READ = "hasAuthority('PERM_learning.featured.manage.any')";
    private static final String WRITE = "hasAuthority('PERM_learning.featured.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)";

    private final IDailyQuizService service;
    private final DailyquizApiMapper dailyquizApiMapper;
    private final FeaturedApiMapper featuredApiMapper;

    @Operation(summary = "List the daily quiz pool", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<List<DailyQuizResponse>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily quiz questions fetched successfully", service.getAll().stream().map(dailyquizApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Preview today's question", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/today")
    @PreAuthorize(READ)
    public ResponseEntity<ApiResponse<DailyQuizResponse>> getToday() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily quiz fetched successfully", dailyquizApiMapper.toResponse(service.getToday())));
    }

    @Operation(summary = "Add a question", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<DailyQuizResponse>> create(@Valid @RequestBody UpsertDailyQuizRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily quiz question created successfully", dailyquizApiMapper.toResponse(service.create(dailyquizApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Update a question", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<DailyQuizResponse>> update(@PathVariable String id, @Valid @RequestBody UpsertDailyQuizRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily quiz question updated successfully", dailyquizApiMapper.toResponse(service.update(id, dailyquizApiMapper.toCommand(request)))));
    }

    @Operation(summary = "Delete a question", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily quiz question deleted successfully", null));
    }

    @Operation(summary = "Reorder the pool (rotation follows this order)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/order")
    @PreAuthorize(WRITE)
    public ResponseEntity<ApiResponse<List<DailyQuizResponse>>> reorder(@Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily quiz questions reordered successfully", service.reorder(featuredApiMapper.toCommand(request)).stream().map(dailyquizApiMapper::toResponse).toList()));
    }
}
