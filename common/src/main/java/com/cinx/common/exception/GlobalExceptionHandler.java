package com.cinx.common.exception;

import com.cinx.common.security.MfaStepUpRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;
import java.util.Locale;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnAuthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.create(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), request);
        ex.getProperties().forEach(problemDetail::setProperty);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAuthorizationDeniedException(AuthorizationDeniedException ignoredEx,
                                                                           HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied", request);
    }

    /** Thrown from a @PreAuthorize expression, so it reaches MVC; without this the catch-all made it a 500. */
    @ExceptionHandler(MfaStepUpRequiredException.class)
    public ResponseEntity<ProblemDetail> handleMfaStepUpRequiredException(MfaStepUpRequiredException ex,
                                                                         HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.MFA_STEP_UP_REQUIRED, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetailFactory.create(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
        ex.getProperties().forEach(problemDetail::setProperty);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    /** Two writes overlapped on a versioned row (BaseEntity @Version): the caller reloads, not a server fault. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Edit conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ErrorCode.EDIT_CONFLICT, "The resource was changed by someone else", request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflictException(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(AlreadyExistException.class)
    public ResponseEntity<ProblemDetail> handleAlreadyExistException(AlreadyExistException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex,
                                                                  HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage(),
                        safeRejectedValue(error.getField(), error.getRejectedValue())))
                .toList();

        ProblemDetail problemDetail = ProblemDetailFactory.validation("Validation failed", errors, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(BindException ex, HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage(),
                        safeRejectedValue(error.getField(), error.getRejectedValue())))
                .toList();

        ProblemDetail problemDetail = ProblemDetailFactory.validation("Validation failed", errors, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex,
                                                                          HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath() == null
                            ? null
                            : violation.getPropertyPath().toString();
                    return new FieldValidationError(
                            field,
                            violation.getMessage(),
                            safeRejectedValue(field, violation.getInvalidValue()));
                })
                .toList();

        ProblemDetail problemDetail = ProblemDetailFactory.validation("Validation failed", errors, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequestFrameworkException(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Spring 6.1+ reports an unmapped path as NoResourceFoundException; without it the catch-all made it a 500. */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ProblemDetail> handleNoHandlerFoundException(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex,
                                                                         HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex,
                                                                            HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(Exception ex, HttpServletRequest request) {
        String traceId = ProblemDetailFactory.traceId();
        log.error("Unexpected error. traceId={}", traceId, ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, ErrorCode code, String detail, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetailFactory.create(status, code, detail, request));
    }

    private Object safeRejectedValue(String field, Object rejectedValue) {
        if (rejectedValue == null || field == null) {
            return rejectedValue;
        }
        String normalizedField = field.toLowerCase(Locale.ROOT);
        if (normalizedField.contains("password")
                || normalizedField.contains("token")
                || normalizedField.contains("secret")
                || normalizedField.contains("credential")
                || normalizedField.contains("authorization")) {
            return "***";
        }
        if (rejectedValue instanceof String
                || rejectedValue instanceof Number
                || rejectedValue instanceof Boolean
                || rejectedValue instanceof Enum<?>) {
            return rejectedValue;
        }
        return null;
    }
}
