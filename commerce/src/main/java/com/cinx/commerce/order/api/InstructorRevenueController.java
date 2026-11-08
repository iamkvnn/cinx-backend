package com.cinx.commerce.order.api;

import com.cinx.commerce.order.api.mapper.InstructorRevenueMapper;
import com.cinx.commerce.order.application.IInstructorRevenueService;

import com.cinx.commerce.order.api.dto.InstructorRevenueResponse;
import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * An instructor's own course revenue. Instructors hold learning.course.update.own; the lines read are always the
 * caller's own (the owner snapshotted on each order line), so nobody sees another instructor's money.
 */
@RestController
@RequestMapping("/api/v1/orders/instructor")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class InstructorRevenueController {
    private final IInstructorRevenueService revenueService;
    private final InstructorRevenueMapper mapper;

    @Operation(summary = "Revenue of your courses for a period, after vouchers and the platform fee")
    @GetMapping("/revenue")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own')")
    ResponseEntity<ApiResponse<InstructorRevenueResponse>> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String courseId,
            @RequestParam(defaultValue = "DAY") String groupBy) {
        if (!"DAY".equalsIgnoreCase(groupBy)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Only groupBy=DAY is supported");
        }
        return ResponseEntity.ok(ApiResponse.success("", mapper.toResponse(revenueService.revenue(AuthenticationUtil.extractUserId(), from, to, courseId))));
    }
}
