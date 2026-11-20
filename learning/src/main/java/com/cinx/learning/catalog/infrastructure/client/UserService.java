package com.cinx.learning.catalog.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.application.query.InstructorLookupView;
import com.cinx.learning.catalog.application.query.UserView;
import com.cinx.learning.config.UserServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user", contextId = "catalogUserClient", path = "/internal",
        configuration = UserServiceFeignConfig.class)
public interface UserService {

    @GetMapping("/users/{userId}")
    ApiResponse<UserView> getInstructorById(@PathVariable String userId);

    @GetMapping("/user-ids")
    ApiResponse<List<UserView>> getInstructorsByIds(@RequestParam List<String> ids);

    @GetMapping("/users/{userId}/instructor-verified")
    ApiResponse<Boolean> isInstructorVerified(@PathVariable String userId);

    /** 404 when no single verified instructor uses the email. */
    @GetMapping("/users/instructor-lookup")
    ApiResponse<InstructorLookupView> lookupInstructor(@RequestParam String email);
}

