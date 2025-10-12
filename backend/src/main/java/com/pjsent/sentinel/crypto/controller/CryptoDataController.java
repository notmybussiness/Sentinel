package com.pjsent.sentinel.crypto.controller;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.dto.CryptoSearchResultDto;
import com.pjsent.sentinel.crypto.dto.TrendingCoinDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 암호화폐 데이터 컨트롤러
 * 암호화폐 가격, 검색, 트렌딩 정보를 제공하는 REST API 엔드포인트
 */
@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Crypto", description = "암호화폐 데이터 API")
public class CryptoDataController {

    private final CryptoDataService cryptoDataService;

    /**
     * 단일 암호화폐 가격 조회
     *
     * GET /api/v1/crypto/price/{symbol}?baseCurrency=KRW
     *
     * @param symbol 암호화폐 심볼 (예: BTC, ETH, XRP)
     * @param baseCurrency 기준 통화 (예: KRW, USDT, BTC) - 기본값 KRW
     * @return 가격 정보
     */
    @GetMapping("/price/{symbol}")
    @Operation(summary = "단일 암호화폐 가격 조회", description = "특정 암호화폐의 현재 가격을 조회합니다.")
    public ResponseEntity<CryptoPriceDto> getCryptoPrice(
            @Parameter(description = "암호화폐 심볼", example = "BTC")
            @PathVariable String symbol,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(defaultValue = "KRW") String baseCurrency) {

        log.info("암호화폐 가격 조회 요청. 심볼: {}, 기준 통화: {}", symbol, baseCurrency);

        try {
            CryptoPriceDto price = cryptoDataService.getCryptoPrice(symbol, baseCurrency);
            return ResponseEntity.ok(price);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터. 심볼: {}, 기준 통화: {}, 오류: {}",
                    symbol, baseCurrency, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("암호화폐 가격 조회 실패. 심볼: {}, 기준 통화: {}, 오류: {}",
                    symbol, baseCurrency, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 배치 암호화폐 가격 조회 (Request Body 방식)
     *
     * POST /api/v1/crypto/prices
     * Body: {
     *   "symbols": ["BTC", "ETH", "XRP"],
     *   "baseCurrency": "KRW"
     * }
     *
     * @param request 배치 조회 요청 (symbols, baseCurrency)
     * @return 가격 정보 목록
     */
    @PostMapping("/prices")
    @Operation(summary = "배치 가격 조회", description = "여러 암호화폐의 현재 가격을 한 번에 조회합니다.")
    public ResponseEntity<List<CryptoPriceDto>> getBatchPrices(
            @RequestBody BatchPriceRequest request) {

        log.info("배치 암호화폐 가격 조회 요청. 개수: {}, 기준 통화: {}",
                request.getSymbols().size(), request.getBaseCurrency());

        try {
            // 입력 검증
            if (request.getSymbols() == null || request.getSymbols().isEmpty()) {
                log.warn("심볼 목록이 비어있습니다.");
                return ResponseEntity.badRequest().build();
            }

            // 최대 개수 제한
            if (request.getSymbols().size() > 100) {
                log.warn("심볼 개수가 너무 많습니다. 요청: {}, 최대: 100", request.getSymbols().size());
                return ResponseEntity.badRequest().build();
            }

            String baseCurrency = request.getBaseCurrency() != null ? request.getBaseCurrency() : "KRW";

            List<CryptoPriceDto> prices = cryptoDataService.getBatchCryptoPrices(
                    request.getSymbols(), baseCurrency);

            log.info("배치 가격 조회 완료. 성공: {}/{}", prices.size(), request.getSymbols().size());
            return ResponseEntity.ok(prices);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터. 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("배치 가격 조회 실패. 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 암호화폐 검색
     *
     * GET /api/v1/crypto/search?query=bitcoin
     *
     * @param query 검색어 (심볼 또는 이름)
     * @return 검색 결과 목록
     */
    @GetMapping("/search")
    @Operation(summary = "암호화폐 검색", description = "심볼 또는 이름으로 암호화폐를 검색합니다.")
    public ResponseEntity<List<CryptoSearchResultDto>> searchCrypto(
            @Parameter(description = "검색어", example = "bitcoin")
            @RequestParam String query) {

        log.info("암호화폐 검색 요청. 키워드: {}", query);

        try {
            // 검색어 검증
            if (query == null || query.trim().isEmpty()) {
                log.warn("검색어가 비어있습니다.");
                return ResponseEntity.badRequest().build();
            }

            List<CryptoSearchResultDto> results = cryptoDataService.searchCrypto(query.trim());

            log.info("검색 완료. 키워드: {}, 결과 수: {}", query, results.size());
            return ResponseEntity.ok(results);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 검색 요청. 키워드: {}, 오류: {}", query, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("검색 실패. 키워드: {}, 오류: {}", query, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 트렌딩 암호화폐 조회 (거래대금 상위)
     *
     * GET /api/v1/crypto/trending?baseCurrency=KRW&limit=10
     *
     * @param baseCurrency 기준 통화 (기본값 KRW)
     * @param limit 조회 개수 (기본값 10, 최대 50)
     * @return 트렌딩 암호화폐 목록
     */
    @GetMapping("/trending")
    @Operation(summary = "트렌딩 암호화폐 조회", description = "거래대금 기준 상위 암호화폐를 조회합니다.")
    public ResponseEntity<List<TrendingCoinDto>> getTrendingCoins(
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(defaultValue = "KRW") String baseCurrency,
            @Parameter(description = "조회 개수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        log.info("트렌딩 암호화폐 조회 요청. 기준 통화: {}, 개수: {}", baseCurrency, limit);

        try {
            // 입력 검증
            if (limit <= 0 || limit > 50) {
                log.warn("잘못된 limit 값. 요청: {}, 허용 범위: 1-50", limit);
                return ResponseEntity.badRequest().build();
            }

            List<TrendingCoinDto> trending = cryptoDataService.getTrendingCoins(baseCurrency, limit);

            log.info("트렌딩 조회 완료. 결과 수: {}", trending.size());
            return ResponseEntity.ok(trending);

        } catch (Exception e) {
            log.error("트렌딩 조회 실패. 기준 통화: {}, 오류: {}", baseCurrency, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 과거 데이터 조회 (캔들 스틱)
     *
     * GET /api/v1/crypto/historical/{symbol}?baseCurrency=KRW&days=30
     *
     * @param symbol 암호화폐 심볼
     * @param baseCurrency 기준 통화
     * @param days 조회 일수 (1~365)
     * @return 과거 가격 데이터 목록
     */
    @GetMapping("/historical/{symbol}")
    @Operation(summary = "과거 데이터 조회", description = "암호화폐의 과거 가격 데이터를 조회합니다.")
    public ResponseEntity<List<CryptoPriceDto>> getHistoricalData(
            @Parameter(description = "암호화폐 심볼", example = "BTC")
            @PathVariable String symbol,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(defaultValue = "KRW") String baseCurrency,
            @Parameter(description = "조회 일수", example = "30")
            @RequestParam(defaultValue = "30") int days) {

        log.info("과거 데이터 조회 요청. 심볼: {}, 기준 통화: {}, 일수: {}", symbol, baseCurrency, days);

        try {
            // 입력 검증
            if (days <= 0 || days > 365) {
                log.warn("잘못된 days 값. 요청: {}, 허용 범위: 1-365", days);
                return ResponseEntity.badRequest().build();
            }

            List<CryptoPriceDto> historical = cryptoDataService.getHistoricalData(symbol, baseCurrency, days);

            log.info("과거 데이터 조회 완료. 심볼: {}, 결과 수: {}", symbol, historical.size());
            return ResponseEntity.ok(historical);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터. 심볼: {}, 오류: {}", symbol, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("과거 데이터 조회 실패. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 서비스 상태 확인
     *
     * GET /api/v1/crypto/status
     *
     * @return 서비스 상태 정보
     */
    @GetMapping("/status")
    @Operation(summary = "서비스 상태 확인", description = "암호화폐 데이터 서비스의 상태를 확인합니다.")
    public ResponseEntity<Object> getServiceStatus() {
        log.info("서비스 상태 확인 요청");

        try {
            boolean isAvailable = cryptoDataService.isServiceAvailable();
            String status = cryptoDataService.getServiceStatus();

            return ResponseEntity.ok()
                    .body(Map.of(
                            "available", isAvailable,
                            "message", status
                    ));

        } catch (Exception e) {
            log.error("서비스 상태 확인 실패. 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "available", false,
                            "message", "서비스 상태 확인 실패"
                    ));
        }
    }

    // ==================== Request DTOs ====================

    /**
     * 배치 가격 조회 요청 DTO
     */
    public static class BatchPriceRequest {
        private List<String> symbols;
        private String baseCurrency;

        public List<String> getSymbols() {
            return symbols;
        }

        public void setSymbols(List<String> symbols) {
            this.symbols = symbols;
        }

        public String getBaseCurrency() {
            return baseCurrency;
        }

        public void setBaseCurrency(String baseCurrency) {
            this.baseCurrency = baseCurrency;
        }
    }
}
