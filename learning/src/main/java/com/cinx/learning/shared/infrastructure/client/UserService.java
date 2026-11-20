package com.cinx.learning.shared.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.courseview.application.query.UserView;
import com.cinx.learning.config.UserServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user", contextId = "learningUserClient", path = "/internal/users",
        configuration = UserServiceFeignConfig.class)
public interface UserService {
    @GetMapping("/{id}")
    ApiResponse<UserView> getUserById(@PathVariable("id") String id);

}
