package com.cinx.notification.notification.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.dto.course.CourseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "learning", contextId = "notificationLearningCatalogClient", path = "/internal/courses",
        configuration = LearningServiceFeignConfig.class)
public interface CourseClient {
    @GetMapping("/{id}")
    ApiResponse<CourseResponse> getCourseById(@PathVariable("id") String id);

    @GetMapping("/{id}/instructor-ids")
    ApiResponse<List<String>> getInstructorIds(@PathVariable("id") String id);
}
