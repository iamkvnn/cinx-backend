package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.api.dto.response.InstructorPublicResponse;
import com.cinx.learning.catalog.application.IInstructorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/instructors")
@RequiredArgsConstructor
public class InstructorPublicController {
    private final IInstructorProfileService instructorProfileService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(summary = "Public page of a verified instructor: profile, figures and categories")
    @GetMapping("/{instructorId}")
    public ResponseEntity<ApiResponse<InstructorPublicResponse>> getPublicProfile(@PathVariable String instructorId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(new ApiResponse<>(true, "Instructor fetched successfully", catalogApiMapper.toResponse(instructorProfileService.getPublicProfile(instructorId))));
    }
}
