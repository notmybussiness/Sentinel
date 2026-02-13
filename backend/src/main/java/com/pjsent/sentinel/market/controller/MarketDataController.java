package com.pjsent.sentinel.market.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pjsent.sentinel.common.exception.ApiErrorResponse;
import com.pjsent.sentinel.market.dto.MarketIndexDto;
import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.ServiceStatusResponse;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Market", description = "Market data APIs")
public class MarketDataController {

    private static final int MAX_BATCH_SYMBOLS = 50;

    private final MarketDataService marketDataService;

    @GetMapping("/price/{symbol}")
    @Operation(summary = "Get latest stock price", description = "Read-only path with no side effects")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock price returned"),
            @ApiResponse(responseCode = "400", description = "Invalid symbol", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Provider error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<StockPriceDto> getStockPrice(
            @Parameter(description = "Ticker symbol", example = "AAPL") @PathVariable String symbol) {
        log.info("Request stock price: {}", symbol);
        return ResponseEntity.ok(marketDataService.getStockPrice(symbol));
    }

    @PostMapping("/price/{symbol}/refresh")
    @Operation(summary = "Refresh stock price and publish event", description = "Explicit write path for refresh + publish")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price refreshed and published"),
            @ApiResponse(responseCode = "400", description = "Invalid symbol", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Provider error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<StockPriceDto> refreshStockPrice(
            @Parameter(description = "Ticker symbol", example = "AAPL") @PathVariable String symbol) {
        log.info("Request stock price refresh: {}", symbol);
        return ResponseEntity.ok(marketDataService.refreshStockPriceAndPublish(symbol));
    }

    @Deprecated
    @GetMapping("/prices")
    @Operation(summary = "Get multiple stock prices (query)", deprecated = true, description = "Deprecated. Use POST /api/v1/market/prices instead")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock prices returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<List<StockPriceDto>> getStockPrices(
            @Parameter(description = "Comma separated symbols", example = "AAPL,MSFT") @RequestParam String symbols) {
        log.info("Request stock prices via query: {}", symbols);

        if (symbols == null || symbols.trim().isEmpty()) {
            throw new IllegalArgumentException("symbols query parameter is required");
        }

        List<String> symbolList = Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        validateBatchSymbols(symbolList);
        return ResponseEntity.ok(marketDataService.getStockPrices(symbolList));
    }

    @PostMapping("/prices")
    @Operation(summary = "Get multiple stock prices (batch)", description = "Canonical batch read path")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock prices returned"),
            @ApiResponse(responseCode = "400", description = "Invalid symbol list", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Provider error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<List<StockPriceDto>> getBatchPrices(@RequestBody List<String> symbols) {
        log.info("Request stock prices via batch body, count: {}", symbols == null ? 0 : symbols.size());
        validateBatchSymbols(symbols);
        return ResponseEntity.ok(marketDataService.getStockPrices(symbols));
    }

    @GetMapping("/indices")
    @Operation(summary = "Get major market indices")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indices returned"),
            @ApiResponse(responseCode = "500", description = "Provider error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<List<MarketIndexDto>> getMarketIndices() {
        log.info("Request market indices");
        return ResponseEntity.ok(marketDataService.getMarketIndices());
    }

    @GetMapping("/search")
    @Operation(summary = "Search symbol")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<List<SearchResultDto>> searchSymbol(
            @Parameter(description = "Keyword", example = "apple") @RequestParam String query) {
        log.info("Request symbol search: {}", query);
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("query parameter is required");
        }
        return ResponseEntity.ok(marketDataService.searchSymbol(query.trim()));
    }

    @GetMapping("/status")
    @Operation(summary = "Get market provider status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service status returned"),
            @ApiResponse(responseCode = "500", description = "Status check failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))) })
    public ResponseEntity<ServiceStatusResponse> getServiceStatus() {
        log.info("Request market service status");
        return ResponseEntity.ok(
                new ServiceStatusResponse(marketDataService.isServiceAvailable(), marketDataService.getProviderStatus()));
    }

    private void validateBatchSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("At least one symbol is required");
        }

        if (symbols.size() > MAX_BATCH_SYMBOLS) {
            throw new IllegalArgumentException("Maximum " + MAX_BATCH_SYMBOLS + " symbols are allowed");
        }
    }
}
