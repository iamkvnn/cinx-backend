package com.cinx.learning.catalog.infrastructure.client;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.shared.application.UpstreamCallException;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorDirectoryAdapterTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private InstructorDirectoryAdapter adapter;

    @Test
    void aSuccessfulAnswerPassesThroughUntouched() {
        ApiResponse<Boolean> answer = new ApiResponse<>(true, "ok", true);
        when(userService.isInstructorVerified("instructor-1")).thenReturn(answer);

        assertThat(adapter.isInstructorVerified("instructor-1")).isSameAs(answer);
    }

    @Test
    void aNotFoundFromTheUserServiceIsTheCallersNotFound() {
        when(userService.getInstructorById("ghost")).thenThrow(feignError(404));

        assertThatThrownBy(() -> adapter.getInstructorById("ghost"))
                .isInstanceOfSatisfying(UpstreamCallException.class, e -> assertThat(e.isNotFound()).isTrue());
    }

    @Test
    void anyOtherFailureIsAnOutageNotANotFound() {
        when(userService.getInstructorsByIds(List.of("a"))).thenThrow(feignError(503));

        assertThatThrownBy(() -> adapter.getInstructorsByIds(List.of("a")))
                .isInstanceOfSatisfying(UpstreamCallException.class, e -> assertThat(e.isNotFound()).isFalse());
    }

    @Test
    void theLookupByEmailTranslatesTheSameWay() {
        when(userService.lookupInstructor("nobody@example.test")).thenThrow(feignError(404));

        assertThatThrownBy(() -> adapter.lookupInstructor("nobody@example.test"))
                .isInstanceOfSatisfying(UpstreamCallException.class, e -> assertThat(e.isNotFound()).isTrue());
    }

    private static FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://user/internal", new HashMap<>(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("call", feign.Response.builder().status(status).reason("x").request(request).headers(new HashMap<>()).build());
    }
}
