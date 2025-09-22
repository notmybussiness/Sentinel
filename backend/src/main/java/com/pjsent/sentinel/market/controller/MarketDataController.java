package com.pjsent.sentinel.market.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시장 데이터 컨트롤러
 * 주식 가격 데이터를 제공하는 REST API 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Slf4j
public class MarketDataController {
    
    private final MarketDataService marketDataService;
    
    /**
     * 단일 주식의 현재 가격을 조회합니다.
     * 
     * @param symbol 주식 심볼 (예: AAPL, MSFT)
     * @return 주식 가격 데이터
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<StockPriceDto> getStockPrice(@PathVariable String symbol) {
        log.info("주식 가격 조회 요청. 심볼: {}", symbol);
        
        try {
            StockPriceDto stockPrice = marketDataService.getStockPrice(symbol);
            return ResponseEntity.ok(stockPrice);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터. 심볼: {}, 오류: {}", symbol, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("주식 가격 조회 실패. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 여러 주식의 현재 가격을 조회합니다.
     * 
     * @param symbols 쉼표로 구분된 주식 심볼 목록 (예: AAPL,MSFT,GOOGL)
     * @return 주식 가격 데이터 목록
     */
    @GetMapping("/prices")
    public ResponseEntity<List<StockPriceDto>> getStockPrices(@RequestParam String symbols) {
        log.info("여러 주식 가격 조회 요청. 심볼: {}", symbols);
        
        try {
            List<String> symbolList = List.of(symbols.split(","));
            List<StockPriceDto> stockPrices = marketDataService.getStockPrices(symbolList);
            return ResponseEntity.ok(stockPrices);
        } catch (Exception e) {
            log.error("여러 주식 가격 조회 실패. 심볼: {}, 오류: {}", symbols, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 서비스 상태를 확인합니다.
     * 
     * @return 서비스 사용 가능 여부
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getServiceStatus() {
        log.info("서비스 상태 확인 요청");
        
        try {
            boolean isAvailable = marketDataService.isServiceAvailable();
            String status = marketDataService.getProviderStatus();
            
            return ResponseEntity.ok()
                    .body(new ServiceStatusResponse(isAvailable, status));
        } catch (Exception e) {
            log.error("서비스 상태 확인 실패. 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ServiceStatusResponse(false, "서비스 상태 확인 실패"));
        }
    }
    
    /**
     * 서비스 상태 응답 DTO
     */
    public static class ServiceStatusResponse {
        private final boolean available;
        private final String message;
        
        public ServiceStatusResponse(boolean available, String message) {
            this.available = available;
            this.message = message;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
