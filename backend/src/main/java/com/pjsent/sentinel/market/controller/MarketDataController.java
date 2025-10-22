package com.pjsent.sentinel.market.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pjsent.sentinel.market.dto.MarketIndexDto;
import com.pjsent.sentinel.market.dto.SearchResultDto;
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
     * 단일 주식의 현재 가격을 조회합니다. (quote 엔드포인트 - 호환성)
     *
     * @param symbol 주식 심볼 (예: AAPL, MSFT, 005930)
     * @return 주식 가격 데이터
     */
    @GetMapping("/quote/{symbol}")
    public ResponseEntity<StockPriceDto> getQuote(@PathVariable String symbol) {
        log.info("주식 시세 조회 요청 (quote). 심볼: {}", symbol);
        
        try {
            StockPriceDto stockPrice = marketDataService.getStockPrice(symbol);
            return ResponseEntity.ok(stockPrice);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터. 심볼: {}, 오류: {}", symbol, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("주식 시세 조회 실패. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 여러 주식의 현재 가격을 조회합니다. (Query Parameter 방식)
     *
     * @param symbols 쉼표로 구분된 주식 심볼 목록 (예: AAPL,MSFT,GOOGL)
     * @return 주식 가격 데이터 목록
     */
    @GetMapping("/prices")
    public ResponseEntity<List<StockPriceDto>> getStockPrices(@RequestParam String symbols) {
        log.info("여러 주식 가격 조회 요청 (Query). 심볼: {}", symbols);

        try {
            List<String> symbolList = List.of(symbols.split(","));
            List<StockPriceDto> stockPrices = marketDataService.getStockPrices(symbolList);
            return ResponseEntity.ok(stockPrices);
        } catch (Exception e) {
            log.error("여러 주식 가격 조회 실패 (Query). 심볼: {}, 오류: {}", symbols, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 배치 주식 가격 조회 (Request Body 방식)
     *
     * Portfolio Holdings의 여러 종목 가격을 한 번에 조회합니다.
     * Query Parameter보다 많은 심볼을 전송할 수 있습니다.
     *
     * 사용 사례:
     * - Portfolio Holdings 실시간 가격 업데이트
     * - 많은 종목을 한 번에 조회 (>20개)
     *
     * 예시 요청:
     * POST /api/v1/market/prices
     * Body: ["AAPL", "MSFT", "GOOGL", "TSLA", "AMZN"]
     *
     * @param symbols 주식 심볼 목록 (JSON 배열)
     * @return 주식 가격 데이터 목록
     */
    @PostMapping("/prices")
    public ResponseEntity<List<StockPriceDto>> getBatchPrices(@RequestBody List<String> symbols) {
        log.info("배치 주식 가격 조회 요청. 심볼 개수: {}, 목록: {}", symbols.size(), symbols);

        try {
            // 입력 검증
            if (symbols == null || symbols.isEmpty()) {
                log.warn("심볼 목록이 비어있습니다.");
                return ResponseEntity.badRequest().build();
            }

            // 최대 개수 제한 (API Rate Limit 고려)
            if (symbols.size() > 50) {
                log.warn("심볼 개수가 너무 많습니다. 요청: {}, 최대: 50", symbols.size());
                return ResponseEntity.badRequest().build();
            }

            // Service Layer 호출
            List<StockPriceDto> stockPrices = marketDataService.getStockPrices(symbols);

            log.info("배치 가격 조회 완료. 성공: {}/{}", stockPrices.size(), symbols.size());
            return ResponseEntity.ok(stockPrices);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터. 심볼: {}, 오류: {}", symbols, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("배치 가격 조회 실패. 심볼: {}, 오류: {}", symbols, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 주요 시장 지수 조회
     *
     * 반환 데이터:
     * - S&P 500 (^GSPC)
     * - NASDAQ (^IXIC)
     * - DOW (^DJI)
     * - KOSPI (^KS11)
     *
     * 동작:
     * 1. Service의 getMarketIndices() 호출
     * 2. 4개 지수 데이터를 List로 반환
     * 3. 조회 실패한 지수는 제외됨
     *
     * @return 시장 지수 목록 (HTTP 200 OK)
     */
    @GetMapping("/indices")
    public ResponseEntity<List<MarketIndexDto>> getMarketIndices() {
        log.info("시장 지수 조회 요청");

        try {
            // Service Layer 호출
            List<MarketIndexDto> indices = marketDataService.getMarketIndices();

            // 성공 응답 (200 OK)
            return ResponseEntity.ok(indices);
        } catch (Exception e) {
            // 에러 로깅 및 500 에러 반환
            log.error("시장 지수 조회 실패. 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 종목 심볼 검색
     *
     * 사용 사례:
     * - Holdings 추가 시 종목 검색
     * - 심볼/회사명으로 검색 가능
     *
     * 동작:
     * 1. Service의 searchSymbol() 호출
     * 2. AlphaVantage Symbol Search API 사용
     * 3. 최대 10개 결과 반환
     *
     * 예시:
     * GET /api/v1/market/search?query=apple
     * GET /api/v1/market/search?query=AAPL
     *
     * @param query 검색 키워드 (필수, 최소 1자)
     * @return 검색 결과 목록 (HTTP 200 OK)
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchResultDto>> searchSymbol(@RequestParam String query) {
        log.info("종목 검색 요청. 키워드: {}", query);

        try {
            // 검색어 검증
            if (query == null || query.trim().isEmpty()) {
                log.warn("검색어가 비어있습니다.");
                return ResponseEntity.badRequest().build();
            }

            // Service Layer 호출
            List<SearchResultDto> results = marketDataService.searchSymbol(query.trim());

            // 성공 응답 (200 OK)
            return ResponseEntity.ok(results);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 검색 요청. 키워드: {}, 오류: {}", query, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("종목 검색 실패. 키워드: {}, 오류: {}", query, e.getMessage(), e);
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
