package com.cinx.learning.engagement.api;

import com.cinx.learning.engagement.api.mapper.EngagementApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.engagement.api.dto.request.StreakSeenRequest;
import com.cinx.learning.engagement.api.dto.response.UserStreakResponse;
import com.cinx.learning.engagement.application.IStreakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning/streaks")
@RequiredArgsConstructor
public class StreakController {

    private final IStreakService streakService;
    private final EngagementApiMapper engagementApiMapper;

    @Operation(summary = "Get current user's streak", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserStreakResponse>> getMyStreak() {
        String userId = AuthenticationUtil.extractUserId();
        UserStreakResponse response = engagementApiMapper.toResponse(streakService.getUserStreak(userId));
        return ResponseEntity.ok(new ApiResponse<>(true, "Streak retrieved successfully", response));
    }

    @Operation(summary = "Record that a streak milestone or a lost streak was shown", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/me/seen")
    public ResponseEntity<ApiResponse<UserStreakResponse>> markSeen(@Valid @RequestBody StreakSeenRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        UserStreakResponse response = engagementApiMapper.toResponse(
                streakService.markSeen(userId, request.milestone(), Boolean.TRUE.equals(request.lost())));
        return ResponseEntity.ok(new ApiResponse<>(true, "Streak moment recorded", response));
    }
}
