package com.codingshuttle.project.uber.uberApp.advices;

import com.codingshuttle.project.uber.uberApp.exceptions.InvalidRideStatusException;
import com.codingshuttle.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.codingshuttle.project.uber.uberApp.exceptions.RuntimeConflictException;
import com.codingshuttle.project.uber.uberApp.exceptions.UnauthorizedAccessException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, exception.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(RuntimeConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeConflictException(RuntimeConflictException exception) {
        log.warn("Conflict: {}", exception.getMessage());
        ApiError apiError = new ApiError(HttpStatus.CONFLICT, exception.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    /**
     * NEW — handles ride state machine violations (e.g. cancelling an ONGOING ride).
     * Returns 400 Bad Request instead of a generic 500, so the frontend can
     * show the user a meaningful error message.
     */
    @ExceptionHandler(InvalidRideStatusException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidRideStatus(InvalidRideStatusException exception) {
        log.warn("Invalid ride status transition: {}", exception.getMessage());
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Bad request argument: {}", exception.getMessage());
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        // Log at debug — this fires on every bad password attempt, don't pollute logs
        log.debug("Authentication failed: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtException(JwtException ex) {
        log.debug("JWT validation failed: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, ex.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, ex.getMessage(), null);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleInputValidationErrors(MethodArgumentNotValidException exception) {
        List<String> errors = exception
                .getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Input validation failed", errors);
        return buildErrorResponseEntity(apiError);
    }

    /**
     * Catch-all handler.
     *
     * FIX: Added structured logging with stack trace so production errors
     * are visible in Railway logs. Previously this swallowed the cause,
     * making LazyInitializationException and race condition failures
     * completely invisible in production logs.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleInternalServerError(Exception exception) {
        log.error("Unhandled exception [{}]: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.", null);
        return buildErrorResponseEntity(apiError);
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }
}
