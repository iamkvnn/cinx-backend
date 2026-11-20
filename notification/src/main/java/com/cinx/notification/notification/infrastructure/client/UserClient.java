package com.cinx.notification.notification.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.dto.user.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user", path = "/internal/users", configuration = UserServiceFeignConfig.class)
public interface UserClient {
    @GetMapping("/{id}")
    ApiResponse<UserDto> getUserById(@PathVariable("id") String id);
}
