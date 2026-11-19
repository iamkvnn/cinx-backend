package com.cinx.notification.notification.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "auth", contextId = "notificationIdentityClient", path = "/internal/identity",
        configuration = IdentityServiceFeignConfig.class)
public interface IdentityClient {
    @GetMapping("/permissions/learning.course.review.any/user-ids")
    ApiResponse<List<String>> getCourseReviewerUserIds();
}
