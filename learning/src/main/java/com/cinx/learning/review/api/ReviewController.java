package com.cinx.learning.review.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.review.api.dto.request.ReviewAnswerRequest;
import com.cinx.learning.review.api.dto.response.ReviewAnswerResponse;
import com.cinx.learning.review.api.dto.response.ReviewTodayResponse;
import com.cinx.learning.review.application.IReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in learner's spaced review of official quiz questions they missed. */
@RestController
@RequestMapping("/api/v1/learning/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final IReviewService reviews;

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<ReviewTodayResponse>> today() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Review queue fetched successfully",
                ReviewTodayResponse.of(reviews.today(AuthenticationUtil.extractUserId()))));
    }

    @PostMapping("/{reviewItemId}/attempts")
    @PreAuthorize("hasAuthority('PERM_learning.progress.write.self')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<ReviewAnswerResponse>> answer(
            @PathVariable String reviewItemId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReviewAnswerRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Review answer recorded",
                ReviewAnswerResponse.of(reviews.answer(AuthenticationUtil.extractUserId(), reviewItemId, request.userAnswer(),
                        request.elapsedSeconds(), idempotencyKey))));
    }
}
