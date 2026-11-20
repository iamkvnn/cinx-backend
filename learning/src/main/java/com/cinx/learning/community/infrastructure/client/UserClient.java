package com.cinx.learning.community.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.community.application.query.UserSummaryView;
import com.cinx.learning.config.UserServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user", contextId = "communityUserClient", path = "/internal",
        configuration = UserServiceFeignConfig.class)
public interface UserClient {
    @GetMapping("/user-ids")
    ApiResponse<List<UserSummaryView>> getUsersByIds(@RequestParam List<String> ids);
}
