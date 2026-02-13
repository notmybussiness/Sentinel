package com.pjsent.sentinel.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Market service availability status")
public class ServiceStatusResponse {

    @Schema(description = "Provider availability", example = "true")
    private final boolean available;

    @Schema(description = "Provider status message", example = "Finnhub (Primary)")
    private final String message;
}
