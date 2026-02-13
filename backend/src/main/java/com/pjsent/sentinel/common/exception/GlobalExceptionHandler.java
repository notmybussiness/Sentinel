package com.pjsent.sentinel.common.exception;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex, WebRequest request) {
        log.warn("No handler found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", "Requested resource was not found", request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex,
            WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });

        return build(HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed", request, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        log.warn("Type mismatch for {}: {}", ex.getName(), ex.getMessage());

        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter '%s' must be of type %s", ex.getName(), requiredType);
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message, request, null);
    }

    @ExceptionHandler({ AuthenticationException.class, BadCredentialsException.class })
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(Exception ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication failed", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Access denied", request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business error: {}", ex.getMessage());

        Map<String, Object> details = null;
        if (ex.getErrorCode() != null) {
            details = new HashMap<>();
            details.put("errorCode", ex.getErrorCode());
            if (ex.getArgs() != null) {
                details.put("args", ex.getArgs());
            }
        }

        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected server error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Unexpected server error occurred", request, null);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message, WebRequest request,
            Map<String, Object> details) {
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(),
                error,
                message,
                request.getDescription(false).replace("uri=", ""));
        body.setDetails(details);

        return ResponseEntity.status(status).body(body);
    }
}
