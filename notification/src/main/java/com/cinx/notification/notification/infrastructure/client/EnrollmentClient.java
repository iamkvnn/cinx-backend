package com.cinx.notification.notification.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "learning", contextId = "notificationLearningEntitlementClient", path = "/internal",
        configuration = LearningServiceFeignConfig.class)
public interface EnrollmentClient {
    @GetMapping("/enrollments/courses/{courseId}/users")
    ApiResponse<java.util.List<String>> getUserIdsEnrolledInCourse(@PathVariable("courseId") String courseId);
}
