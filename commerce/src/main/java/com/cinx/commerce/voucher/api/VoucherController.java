package com.cinx.commerce.voucher.api;

import com.cinx.commerce.voucher.api.dto.CreateVoucherRequest;
import com.cinx.commerce.voucher.api.dto.UpdateVoucherRequest;
import com.cinx.commerce.voucher.api.dto.VoucherPreviewResponse;
import com.cinx.commerce.voucher.api.dto.VoucherResponse;
import com.cinx.commerce.voucher.api.mapper.VoucherMapper;
import com.cinx.commerce.voucher.application.IVoucherService;
import com.cinx.common.dto.*;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.common.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/vouchers") @RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class VoucherController {
    private final IVoucherService service;
    private final VoucherMapper mapper;

    @GetMapping @PreAuthorize("hasAuthority('PERM_commerce.voucher.manage.any')")
    ResponseEntity<PaginatedApiResponse<VoucherResponse>> findAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int limit) {
        // the API counts pages from zero, the application from one
        PageResult<VoucherResponse> result = service.findAll(new PageQuery(page + 1, limit, null)).map(mapper::toResponse);
        return ResponseEntity.ok(new PaginatedApiResponse<>(true, "Vouchers fetched successfully", result.content(),
                new PaginatedMetadata(page, limit, result.totalElements(), result.totalPages())));
    }

    @GetMapping("/{code}") @PreAuthorize("hasAuthority('PERM_commerce.voucher.manage.any')")
    ResponseEntity<ApiResponse<VoucherResponse>> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success("Voucher fetched successfully", mapper.toResponse(service.findByCode(code))));
    }

    @GetMapping("/{code}/preview")
    ResponseEntity<ApiResponse<VoucherPreviewResponse>> preview(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success("Voucher preview calculated",
                mapper.toResponse(service.preview(AuthenticationUtil.extractUserId(), code))));
    }

    @PostMapping @PreAuthorize("hasAuthority('PERM_commerce.voucher.manage.any')")
    ResponseEntity<ApiResponse<VoucherResponse>> create(@Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Voucher created successfully",
                mapper.toResponse(service.create(mapper.toCommand(request)))));
    }

    @PutMapping("/{code}") @PreAuthorize("hasAuthority('PERM_commerce.voucher.manage.any')")
    ResponseEntity<ApiResponse<VoucherResponse>> update(@PathVariable String code, @Valid @RequestBody UpdateVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Voucher updated successfully",
                mapper.toResponse(service.update(code, mapper.toCommand(request)))));
    }

    @PatchMapping("/{code}/active") @PreAuthorize("hasAuthority('PERM_commerce.voucher.manage.any')")
    ResponseEntity<ApiResponse<VoucherResponse>> setActive(@PathVariable String code, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Voucher status updated", mapper.toResponse(service.setActive(code, active))));
    }
}
