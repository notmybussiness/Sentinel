package com.pjsent.sentinel.portfolio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pjsent.sentinel.common.exception.ApiErrorResponse;
import com.pjsent.sentinel.portfolio.dto.AddHoldingRequest;
import com.pjsent.sentinel.portfolio.dto.CreatePortfolioRequest;
import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.dto.PortfolioHoldingDto;
import com.pjsent.sentinel.portfolio.dto.UpdateHoldingRequest;
import com.pjsent.sentinel.portfolio.dto.UpdatePortfolioRequest;
import com.pjsent.sentinel.portfolio.service.PortfolioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio", description = "Portfolio management APIs")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @Operation(summary = "Get all portfolios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolios returned"),
            @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<List<PortfolioDto>> getPortfolios(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Request portfolios for user: {}", userId);
        return ResponseEntity.ok(portfolioService.getPortfoliosByUserId(userId));
    }

    @GetMapping("/{portfolioId}")
    @Operation(summary = "Get single portfolio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio returned"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<PortfolioDto> getPortfolio(@PathVariable Long portfolioId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Request portfolio {} for user {}", portfolioId, userId);
        return ResponseEntity.ok(portfolioService.getPortfolioById(portfolioId, userId));
    }

    @PostMapping
    @Operation(summary = "Create portfolio")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Portfolio created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<PortfolioDto> createPortfolio(Authentication authentication,
            @Valid @RequestBody CreatePortfolioRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Create portfolio for user {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.createPortfolio(userId, request));
    }

    @PutMapping("/{portfolioId}")
    @Operation(summary = "Update portfolio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<PortfolioDto> updatePortfolio(@PathVariable Long portfolioId, Authentication authentication,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update portfolio {} for user {}", portfolioId, userId);
        return ResponseEntity.ok(portfolioService.updatePortfolio(portfolioId, userId, request));
    }

    @DeleteMapping("/{portfolioId}")
    @Operation(summary = "Delete portfolio")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Portfolio deleted"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long portfolioId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Delete portfolio {} for user {}", portfolioId, userId);
        portfolioService.deletePortfolio(portfolioId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{portfolioId}/holdings")
    @Operation(summary = "Add holding")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Holding added"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<PortfolioHoldingDto> addHolding(@PathVariable Long portfolioId, Authentication authentication,
            @Valid @RequestBody AddHoldingRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Add holding to portfolio {} for user {}", portfolioId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.addHolding(portfolioId, userId, request));
    }

    @PutMapping("/{portfolioId}/holdings/{holdingId}")
    @Operation(summary = "Update holding")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Holding updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Holding not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<PortfolioHoldingDto> updateHolding(@PathVariable Long portfolioId, @PathVariable Long holdingId,
            Authentication authentication, @Valid @RequestBody UpdateHoldingRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update holding {} in portfolio {} for user {}", holdingId, portfolioId, userId);
        return ResponseEntity.ok(portfolioService.updateHolding(portfolioId, holdingId, userId, request));
    }

    @DeleteMapping("/{portfolioId}/holdings/{holdingId}")
    @Operation(summary = "Delete holding")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Holding deleted"),
            @ApiResponse(responseCode = "404", description = "Holding not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<Void> deleteHolding(@PathVariable Long portfolioId, @PathVariable Long holdingId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Delete holding {} in portfolio {} for user {}", holdingId, portfolioId, userId);
        portfolioService.deleteHolding(portfolioId, holdingId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{portfolioId}/recalculate")
    @Operation(summary = "Recalculate portfolio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio recalculated"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<PortfolioDto> recalculatePortfolio(@PathVariable Long portfolioId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Recalculate portfolio {} for user {}", portfolioId, userId);
        return ResponseEntity.ok(portfolioService.recalculatePortfolio(portfolioId, userId));
    }
}
