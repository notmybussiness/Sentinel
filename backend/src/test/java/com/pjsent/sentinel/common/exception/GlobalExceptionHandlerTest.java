package com.pjsent.sentinel.common.exception;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 단위 테스트
 * 
 * TDD Cycle: RED phase - 모든 예외 핸들러에 대한 테스트 작성
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 테스트")
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

    // ========================================================================
    // 404 Not Found Tests
    // ========================================================================

    @Nested
    @DisplayName("404 Not Found 처리")
    class NotFoundTests {

        @Test
        @DisplayName("NoHandlerFoundException 발생 시 404 반환")
        void handleNoHandlerFound_ShouldReturn404() {
            // Given
            NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/unknown", null);

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleNoHandlerFound(ex,
                    webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getError()).isEqualTo("Not Found");
            assertThat(response.getBody().getPath()).isEqualTo(TEST_URI);
        }

        @Test
        @DisplayName("ResourceNotFoundException 발생 시 404 반환 및 메시지 포함")
        void handleResourceNotFound_ShouldReturn404WithMessage() {
            // Given
            String errorMessage = "Portfolio not found with id: 123";
            ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex,
                    webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
        }
    }

    // ========================================================================
    // 400 Bad Request Tests
    // ========================================================================

    @Nested
    @DisplayName("400 Bad Request 처리")
    class BadRequestTests {

        @Test
        @DisplayName("IllegalArgumentException 발생 시 400 반환")
        void handleIllegalArgument_ShouldReturn400() {
            // Given
            String errorMessage = "Invalid portfolio name";
            IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex,
                    webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("MethodArgumentNotValidException 발생 시 400 반환 및 필드 에러 포함")
        void handleValidationExceptions_ShouldReturnFieldErrors() {
            // Given
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError1 = new FieldError("portfolioDto", "name", "Name is required");
            FieldError fieldError2 = new FieldError("portfolioDto", "amount", "Amount must be positive");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler
                    .handleValidationExceptions(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
            assertThat(response.getBody().getDetails()).isNotNull();
            assertThat(response.getBody().getDetails()).containsKey("name");
            assertThat(response.getBody().getDetails()).containsKey("amount");
        }

        @Test
        @DisplayName("MethodArgumentTypeMismatchException 발생 시 400 반환 및 파라미터 정보 포함")
        void handleTypeMismatch_ShouldReturnParameterInfo() {
            // Given
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getName()).thenReturn("portfolioId");
            when(ex.getRequiredType()).thenReturn((Class) Long.class);

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleTypeMismatch(ex,
                    webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).contains("portfolioId");
            assertThat(response.getBody().getMessage()).contains("Long");
        }

        @Test
        @DisplayName("BusinessException 발생 시 400 반환 및 에러 코드 포함")
        void handleBusinessException_ShouldReturnErrorCode() {
            // Given
            String errorCode = "PORTFOLIO_LIMIT_EXCEEDED";
            String message = "포트폴리오 생성 한도를 초과했습니다.";
            BusinessException ex = new BusinessException(errorCode, message);

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleBusinessException(ex,
                    webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo(message);
            assertThat(response.getBody().getDetails()).isNotNull();
            assertThat(response.getBody().getDetails().get("errorCode")).isEqualTo(errorCode);
        }
    }

    // ========================================================================
    // 401 Unauthorized Tests
    // ========================================================================

    @Nested
    @DisplayName("401 Unauthorized 처리")
    class UnauthorizedTests {

        @Test
        @DisplayName("AuthenticationException 발생 시 401 반환")
        void handleAuthenticationException_ShouldReturn401() {
            // Given
            AuthenticationException ex = new AuthenticationException("Token expired") {
            };

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler
                    .handleAuthenticationException(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
            assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("BadCredentialsException 발생 시 401 반환")
        void handleBadCredentials_ShouldReturn401() {
            // Given
            BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler
                    .handleAuthenticationException(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
        }
    }

    // ========================================================================
    // 403 Forbidden Tests
    // ========================================================================

    @Nested
    @DisplayName("403 Forbidden 처리")
    class ForbiddenTests {

        @Test
        @DisplayName("AccessDeniedException 발생 시 403 반환")
        void handleAccessDenied_ShouldReturn403() {
            // Given
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleAccessDenied(ex,
                    webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(403);
            assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        }
    }

    // ========================================================================
    // 500 Internal Server Error Tests
    // ========================================================================

    @Nested
    @DisplayName("500 Internal Server Error 처리")
    class InternalServerErrorTests {

        @Test
        @DisplayName("예상치 못한 Exception 발생 시 500 반환")
        void handleGenericException_ShouldReturn500() {
            // Given
            Exception ex = new RuntimeException("Unexpected database error");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleGenericException(ex,
                    webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
            // 보안상 상세 메시지는 노출하지 않음
            assertThat(response.getBody().getMessage()).doesNotContain("database");
        }
    }
}
