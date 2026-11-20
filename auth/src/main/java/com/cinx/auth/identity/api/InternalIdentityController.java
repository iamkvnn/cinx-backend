package com.cinx.auth.identity.api;

import com.cinx.auth.identity.application.IIdentityService;

import com.cinx.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Hidden
@RestController
@RequestMapping("/internal/identity")
@RequiredArgsConstructor
public class InternalIdentityController {
    private final IIdentityService identityService;

    @GetMapping("/permissions/{permission}/user-ids")
    @PreAuthorize("hasAuthority('PERM_auth.internal.permission-members.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-notification-service')")
    ApiResponse<List<String>> findUsersByPermission(@PathVariable String permission) {
        return ApiResponse.success("Permission members fetched successfully",
                identityService.findUserIdsByPermission(permission));
    }
}
