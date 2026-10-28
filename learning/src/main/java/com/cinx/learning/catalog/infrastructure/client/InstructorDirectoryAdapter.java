package com.cinx.learning.catalog.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.application.InstructorDirectory;
import com.cinx.learning.catalog.application.query.InstructorLookupView;
import com.cinx.learning.catalog.application.query.UserView;
import com.cinx.learning.shared.application.UpstreamCallException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
class InstructorDirectoryAdapter implements InstructorDirectory {
    private final UserService userService;

    @Override
    public ApiResponse<UserView> getInstructorById(String userId) {
        return call(() -> userService.getInstructorById(userId));
    }

    @Override
    public ApiResponse<List<UserView>> getInstructorsByIds(List<String> ids) {
        return call(() -> userService.getInstructorsByIds(ids));
    }

    @Override
    public ApiResponse<Boolean> isInstructorVerified(String userId) {
        return call(() -> userService.isInstructorVerified(userId));
    }

    @Override
    public ApiResponse<InstructorLookupView> lookupInstructor(String email) {
        return call(() -> userService.lookupInstructor(email));
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (FeignException e) {
            throw new UpstreamCallException(e instanceof FeignException.NotFound, e);
        }
    }
}
