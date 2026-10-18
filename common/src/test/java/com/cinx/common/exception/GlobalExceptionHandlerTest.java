package com.cinx.common.exception;

import com.cinx.common.security.MfaStepUpRequiredException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundUsesGenericResourceNotFoundCode() {
        MockHttpServletRequest request = request("/api/v1/courses/missing");

        var response = handler.handleNotFoundException(new NotFoundException("Course not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getType().toString()).isEqualTo("urn:cinx:problem:resource-not-found");
        assertThat(body.getTitle()).isEqualTo("Resource not found");
        assertThat(body.getDetail()).isEqualTo("Course not found");
        assertThat(body.getInstance().toString()).isEqualTo("/api/v1/courses/missing");
        assertThat(body.getProperties()).containsEntry("code", "RESOURCE_NOT_FOUND");
        assertThat(body.getProperties()).containsKeys("timestamp", "traceId");
    }

    @Test
    void businessErrorUsesSpecificCode() {
        MockHttpServletRequest request = request("/api/v1/identity/accounts/user-1/link");

        var response = handler.handleBadRequestException(
                new BadRequestException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"),
                request);

        ProblemDetail body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getType().toString()).isEqualTo("urn:cinx:problem:invalid-credentials");
        assertThat(body.getProperties()).containsEntry("code", "INVALID_CREDENTIALS");
    }

    @Test
    void validationErrorsIncludeFieldListAndMaskSensitiveValues() throws Exception {
        MockHttpServletRequest request = request("/api/v1/profiles/me/onboarding");
        PasswordRequest target = new PasswordRequest("secret");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "password", "secret", false, null, null, "password is required"));
        bindingResult.addError(new FieldError("request", "name", "Ada", false, null, null, "name is invalid"));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyEndpoint", PasswordRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        var response = handler.handleValidationException(exception, request);

        ProblemDetail body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getProperties()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(body.getProperties()).containsKey("errors");

        @SuppressWarnings("unchecked")
        List<FieldValidationError> errors = (List<FieldValidationError>) body.getProperties().get("errors");
        assertThat(errors).extracting(FieldValidationError::field).containsExactly("password", "name");
        assertThat(errors.get(0).rejectedValue()).isEqualTo("***");
        assertThat(errors.get(1).rejectedValue()).isEqualTo("Ada");
    }

    @Test
    void unexpectedExceptionDoesNotLeakInternalMessage() {
        MockHttpServletRequest request = request("/api/v1/test");

        var response = handler.handleGeneralException(new IllegalStateException("database password leaked"), request);

        ProblemDetail body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(body.getProperties()).containsEntry("code", "INTERNAL_ERROR");
    }

    @Test
    void aMissingMfaStepUpIsForbiddenWithItsOwnCode() {
        MockHttpServletRequest request = request("/api/v1/reporting/overview");

        var response = handler.handleMfaStepUpRequiredException(new MfaStepUpRequiredException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "MFA_STEP_UP_REQUIRED");
    }

    @SuppressWarnings("unused")
    private void dummyEndpoint(PasswordRequest request) {
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private record PasswordRequest(String password) {
    }
}
