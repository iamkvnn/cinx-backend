package com.cinx.learning.engagement.api;

import com.cinx.learning.engagement.api.mapper.EngagementApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.engagement.api.dto.request.SetDailyGoalRequest;
import com.cinx.learning.engagement.api.dto.response.DailyGoalResponse;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.engagement.application.query.DailyGoalView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/learning/daily-goals")
@RequiredArgsConstructor
public class DailyGoalController {

    private final IDailyGoalService dailyGoalService;
    private final EngagementApiMapper engagementApiMapper;

    @Operation(summary = "Get current user's daily goals for a specific date", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ApiResponse<List<DailyGoalResponse>> getDailyGoals(@RequestParam(required = false) LocalDate date) {
        String userId = AuthenticationUtil.extractUserId();
        LocalDate goalDate = date != null ? date : LocalDate.now();
        List<DailyGoalResponse> responses = dailyGoalService.getUserDailyGoals(userId, goalDate)
                .stream()
                .map(engagementApiMapper::toResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(true, "Daily goals fetched successfully", responses);
    }

    @Operation(summary = "Get current user's daily goals for a specific month", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/month")
    public ApiResponse<List<DailyGoalResponse>> getDailyGoalsInMonth(@RequestParam int year, @RequestParam int month) {
        String userId = AuthenticationUtil.extractUserId();
        List<DailyGoalView> goals = dailyGoalService.getUserDailyGoalsInMonth(userId, year, month);
        
        List<DailyGoalResponse> responses = goals.stream().map(engagementApiMapper::toResponse).collect(Collectors.toList());
        
        return new ApiResponse<>(true, "Monthly daily goals fetched successfully", responses);
    }

    @Operation(summary = "Set current user's daily goal target", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ApiResponse<DailyGoalResponse> setDailyGoal(@Valid @RequestBody SetDailyGoalRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        DailyGoalResponse response = engagementApiMapper.toResponse(dailyGoalService.setDailyGoal(userId, engagementApiMapper.toCommand(request)));
        
        return new ApiResponse<>(true, "Daily goal updated successfully", response);
    }

    @Operation(summary = "Edit current user's daily goal target", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping
    public ApiResponse<DailyGoalResponse> editDailyGoal(@Valid @RequestBody SetDailyGoalRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        DailyGoalResponse response = engagementApiMapper.toResponse(dailyGoalService.setDailyGoal(userId, engagementApiMapper.toCommand(request)));
        
        return new ApiResponse<>(true, "Daily goal updated successfully", response);
    }

    @Operation(summary = "Delete current user's daily goal for a specific date", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping
    public ApiResponse<Void> deleteDailyGoal(
            @RequestParam(required = false) LocalDate date,
            @RequestParam DailyGoalType goalType,
            @RequestParam(required = false) String targetItemId) {
        String userId = AuthenticationUtil.extractUserId();
        dailyGoalService.deleteDailyGoal(userId, date, goalType, targetItemId);
        return new ApiResponse<>(true, "Daily goal deleted successfully", null);
    }
}
