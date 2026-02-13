package com.pjsent.sentinel.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Standard API error response")
public class ApiErrorResponse {

    @Schema(description = "Error timestamp", example = "2026-02-13T10:15:30")
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error name", example = "Bad Request")
    private String error;

    @Schema(description = "Error message", example = "Invalid request parameter")
    private String message;

    @Schema(description = "Request path", example = "/api/v1/market/price/AAPL")
    private String path;

    @Schema(description = "Optional details for validation or business errors")
    private Map<String, Object> details;

    public ApiErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
