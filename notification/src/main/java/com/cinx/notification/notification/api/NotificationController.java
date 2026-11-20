package com.cinx.notification.notification.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.notification.notification.api.dto.response.NotificationCountsResponse;
import com.cinx.notification.notification.api.dto.response.UserNotificationResponse;
import com.cinx.notification.notification.api.dto.request.CreateNotificationRequest;
import com.cinx.notification.notification.api.mapper.UserNotificationMapper;
import com.cinx.notification.notification.application.INotificationService;
import com.cinx.notification.dispatch.application.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import com.cinx.common.utils.AuthenticationUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final INotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final UserNotificationMapper userNotificationMapper;

    @Value("${cinx.notification.test-endpoints-enabled:false}")
    private boolean testEndpointsEnabled;

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_notification.read.self')")
    public ResponseEntity<PaginatedApiResponse<UserNotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sort
    ) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(PaginationWrapper.wrap(
                notificationService.getNotifications(userId, query, isRead, type, page, size, sort).map(userNotificationMapper::toDto)));
    }

    @Operation(summary = "Counts for the notification inbox: total, unread and per type", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/counts")
    @PreAuthorize("hasAuthority('PERM_notification.read.self')")
    public ResponseEntity<ApiResponse<NotificationCountsResponse>> getCounts() {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification counts retrieved successfully",
                userNotificationMapper.toDto(notificationService.getCounts(userId))));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('PERM_notification.read.self')")
    public ResponseEntity<ApiResponse<Long>> countUnreadNotifications() {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread notifications count retrieved successfully", notificationService.countUnreadNotifications(userId)));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{notificationId}/toggle-read")
    @PreAuthorize("hasAuthority('PERM_notification.update.self')")
    public ResponseEntity<ApiResponse<Void>> toggleRead(@PathVariable String notificationId) {
        String userId = AuthenticationUtil.extractUserId();
        notificationService.toggleRead(userId, notificationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification read status toggled successfully", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/mark-all-read")
    @PreAuthorize("hasAuthority('PERM_notification.update.self')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        String userId = AuthenticationUtil.extractUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as read successfully", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/mark-all-unread")
    @PreAuthorize("hasAuthority('PERM_notification.update.self')")
    public ResponseEntity<ApiResponse<Void>> markAllAsUnread() {
        String userId = AuthenticationUtil.extractUserId();
        notificationService.markAllAsUnread(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as unread successfully", null));
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAuthority('PERM_notification.update.self')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable String notificationId) {
        String userId = AuthenticationUtil.extractUserId();
        notificationService.deleteNotification(userId, notificationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification deleted successfully", null));
    }

    @Operation(summary = "Test Push Notification", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/test-push")
    @PreAuthorize("hasAuthority('PERM_notification.test.send.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<Void>> testPushNotification(@RequestParam String title, @RequestParam String body) {
        ensureTestEndpointsEnabled();
        String userId = AuthenticationUtil.extractUserId();
        pushNotificationService.sendPushNotificationToUser(userId, title, body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Push notification sent", null));
    }

    @PostMapping("/test-in-app")
    @PreAuthorize("hasAuthority('PERM_notification.test.send.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<Void>> testInAppNotification(@RequestParam String userId, @RequestParam String title, @RequestParam String body) {
        ensureTestEndpointsEnabled();
        notificationService.sendInApp(userNotificationMapper.toCommand(new CreateNotificationRequest(title, body, List.of(userId))));
        return ResponseEntity.ok(new ApiResponse<>(true, "In-app notification sent", null));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAuthority('PERM_notification.broadcast.send.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<Void>> broadcast(@Valid @RequestBody CreateNotificationRequest request) {
        notificationService.sendInApp(userNotificationMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Broadcast sent", null));
    }

    private void ensureTestEndpointsEnabled() {
        if (!testEndpointsEnabled) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Notification test endpoints are disabled");
        }
    }
}
