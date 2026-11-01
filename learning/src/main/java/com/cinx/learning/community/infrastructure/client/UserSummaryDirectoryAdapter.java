package com.cinx.learning.community.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.community.application.UserSummaryDirectory;
import com.cinx.learning.community.application.query.UserSummaryView;
import com.cinx.learning.shared.application.UpstreamCallException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class UserSummaryDirectoryAdapter implements UserSummaryDirectory {
    private final UserClient userClient;

    @Override
    public ApiResponse<List<UserSummaryView>> getUsersByIds(List<String> ids) {
        try {
            return userClient.getUsersByIds(ids);
        } catch (FeignException e) {
            throw new UpstreamCallException(e instanceof FeignException.NotFound, e);
        }
    }
}
