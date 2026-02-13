package com.pjsent.sentinel.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    private static final String TEST_URI = "/api/v1/test";

    @BeforeEach
    void setUp() {
        when(webRequest.getDescription(false)).thenReturn("uri=" + TEST_URI);
    }

    @Nested
    @DisplayName("Not Found handlers")
    class NotFoundTests {

        @Test
        @DisplayName("NoHandlerFoundException returns 404")
        void handleNoHandlerFoundShouldReturn404() {
            NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/unknown", null);

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNoHandlerFound(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getError()).isEqualTo("Not Found");
            assertThat(response.getBody().getPath()).isEqualTo(TEST_URI);
        }

        @Test
        @DisplayName("ResourceNotFoundException returns 404")
        void handleResourceNotFoundShouldReturn404() {
            String errorMessage = "Portfolio not found with id: 123";
            ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Bad Request handlers")
    class BadRequestTests {

        @Test
        @DisplayName("IllegalArgumentException returns 400")
        void handleIllegalArgumentShouldReturn400() {
            String errorMessage = "Invalid portfolio name";
            IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("Validation exception returns field errors")
        void handleValidationExceptionsShouldReturnFieldErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError1 = new FieldError("portfolioDto", "name", "Name is required");
            FieldError fieldError2 = new FieldError("portfolioDto", "amount", "Amount must be positive");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidationExceptions(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
            assertThat(response.getBody().getDetails()).isNotNull();
            assertThat(response.getBody().getDetails()).containsKeys("name", "amount");
        }

        @Test
        @DisplayName("Type mismatch returns parameter information")
        void handleTypeMismatchShouldReturnParameterInfo() {
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getName()).thenReturn("portfolioId");
            when(ex.getRequiredType()).thenReturn((Class) Long.class);

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleTypeMismatch(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).contains("portfolioId");
            assertThat(response.getBody().getMessage()).contains("Long");
        }

        @Test
        @DisplayName("BusinessException returns errorCode in details")
        void handleBusinessExceptionShouldReturnErrorCode() {
            String errorCode = "PORTFOLIO_LIMIT_EXCEEDED";
            String message = "Portfolio creation limit exceeded";
            BusinessException ex = new BusinessException(errorCode, message);

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBusinessException(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo(message);
            assertThat(response.getBody().getDetails()).isNotNull();
            assertThat(response.getBody().getDetails().get("errorCode")).isEqualTo(errorCode);
        }
    }

    @Nested
    @DisplayName("Unauthorized handlers")
    class UnauthorizedTests {

        @Test
        @DisplayName("AuthenticationException returns 401")
        void handleAuthenticationExceptionShouldReturn401() {
            AuthenticationException ex = new AuthenticationException("Token expired") {
            };

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
            assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("BadCredentialsException returns 401")
        void handleBadCredentialsShouldReturn401() {
            BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("Forbidden handler")
    class ForbiddenTests {

        @Test
        @DisplayName("AccessDeniedException returns 403")
        void handleAccessDeniedShouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAccessDenied(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(403);
            assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        }
    }

    @Nested
    @DisplayName("Internal server error handler")
    class InternalServerErrorTests {

        @Test
        @DisplayName("Unexpected exception returns 500")
        void handleGenericExceptionShouldReturn500() {
            Exception ex = new RuntimeException("Unexpected database error");

            ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleGenericException(ex, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getMessage()).doesNotContain("database");
        }
    }
}
