package com.cinx.learning.shared.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.courseview.application.query.UserView;
import com.cinx.learning.shared.application.UpstreamCallException;
import com.cinx.learning.shared.application.UserDirectory;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserDirectoryAdapter implements UserDirectory {
    private final UserService userService;

    @Override
    public ApiResponse<UserView> getUserById(String id) {
        try {
            return userService.getUserById(id);
        } catch (FeignException e) {
            throw new UpstreamCallException(e instanceof FeignException.NotFound, e);
        }
    }
}
