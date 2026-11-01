package com.cinx.learning.notificationprojection.api.internal;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.notificationprojection.application.INotificationProjectionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class NotificationProjectionController {
    private final INotificationProjectionService service;

    @GetMapping("/enrollments/courses/{courseId}/users")
    @PreAuthorize("hasAuthority('PERM_learning.internal.enrollment.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-notification-service', 'cinx-reporting-service')")
    public ResponseEntity<ApiResponse<List<String>>> getEnrolledUsers(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.success("Enrolled users fetched", service.getEnrolledUserIds(courseId)));
    }
}
