package com.cinx.learning.dailyquiz.api;

import com.cinx.learning.dailyquiz.api.mapper.DailyquizApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.dailyquiz.api.dto.DailyQuizResponse;
import com.cinx.learning.dailyquiz.application.IDailyQuizService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DailyQuizController {
    private final IDailyQuizService dailyQuizService;
    private final DailyquizApiMapper dailyquizApiMapper;

    @Operation(summary = "Today's teaser question for the public home page (404 when none is configured)")
    @GetMapping("/api/v1/assessments/daily-quiz")
    public ResponseEntity<ApiResponse<DailyQuizResponse>> getToday() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily quiz fetched successfully", dailyquizApiMapper.toResponse(dailyQuizService.getToday())));
    }
}
