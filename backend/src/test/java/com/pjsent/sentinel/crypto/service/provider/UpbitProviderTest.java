package com.pjsent.sentinel.crypto.service.provider;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * UpbitProvider 단위 테스트
 *
 * TDD Cycle:
 * [RED] 테스트 먼저 작성 → Upbit API 호출 미구현 시 실패
 * [GREEN] UpbitProvider 구현으로 통과
 * [REFACTOR] 파싱 로직 및 에러 핸들링 개선
 *
 * 테스트 범위:
 * - getCryptoPrice() - 단일 암호화폐 가격 조회
 * - getBatchCryptoPrices() - 배치 가격 조회
 * - 에러 시나리오 (API 실패, 빈 응답, 네트워크 오류)
 * - 데이터 파싱 정확성
 */
@ExtendWith(MockitoExtension.class)
class UpbitProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UpbitProvider upbitProvider;

    private static final String BASE_URL = "https://api.upbit.com/v1";

    @BeforeEach
    void setUp() {
        // @Value 필드에 테스트 값 주입
        ReflectionTestUtils.setField(upbitProvider, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(upbitProvider, "enabled", true);
        ReflectionTestUtils.setField(upbitProvider, "timeout", 10000);
    }

    // ==================== 📝 여기서부터 함께 작성합니다! ====================

    @Test
    @DisplayName("[성공] BTC 가격 조회 - 정상 응답 시 CryptoPriceDto 반환")
    void getCryptoPrice_should_return_price_when_api_returns_valid_data() {
        // ========================================
        // Step 1: Given - Mock 응답 데이터 준비 (Helper 메서드 활용!)
        // ========================================
        String symbol = "BTC";
        String baseCurrency = "KRW";

        // Helper 메서드로 Mock 데이터 생성
        Map<String, Object> mockTickerData = createDummyTickerData("BTC", 50000000.0);

        // RestTemplate이 반환할 ResponseEntity 생성
        ResponseEntity<List> mockResponse = ResponseEntity.ok(List.of(mockTickerData));

        // ========================================
        // Step 2: When - RestTemplate Mock 설정
        // ========================================
        // "RestTemplate.getForEntity()가 호출되면 mockResponse를 반환해라"
        String expectedUrl = BASE_URL + "/ticker?markets=KRW-BTC";
        when(restTemplate.getForEntity(eq(expectedUrl), eq(List.class)))
                .thenReturn(mockResponse);

        // ========================================
        // Step 3: 실행 - 실제 메서드 호출
        // ========================================
        CryptoPriceDto result = upbitProvider.getCryptoPrice(symbol, baseCurrency);

        // ========================================
        // Step 4: Then - 검증 (핵심 필드만!)
        // ========================================
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("BTC");
        assertThat(result.getBaseCurrency()).isEqualTo("KRW");
        assertThat(result.getMarketCode()).isEqualTo("KRW-BTC");
        assertThat(result.getPrice()).isEqualTo(50000000.0);
        assertThat(result.getProvider()).isEqualTo("Upbit");
    }

    @Test
    @DisplayName("[실패] API 응답이 빈 리스트면 RuntimeException 발생")
    void getCryptoPrice_should_throw_exception_when_response_is_empty() {
        // Given
        String symbol = "BTC";
        String baseCurrency = "KRW";

        // 빈 리스트 응답
        ResponseEntity<List> emptyResponse = ResponseEntity.ok(List.of());

        when(restTemplate.getForEntity(anyString(), eq(List.class)))
                .thenReturn(emptyResponse);

        // When & Then
        assertThatThrownBy(() -> upbitProvider.getCryptoPrice(symbol, baseCurrency))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Upbit API 응답 오류");
    }

    @Test
    @DisplayName("[실패] API 호출 시 네트워크 오류 발생하면 RuntimeException")
    void getCryptoPrice_should_throw_exception_when_network_error_occurs() {
        // Given
        String symbol = "BTC";
        String baseCurrency = "KRW";

        // RestTemplate이 네트워크 오류를 던지도록 설정
        when(restTemplate.getForEntity(anyString(), eq(List.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        // When & Then
        assertThatThrownBy(() -> upbitProvider.getCryptoPrice(symbol, baseCurrency))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Upbit API 호출 실패");
    }

    @Test
    @DisplayName("[실패] Provider가 비활성화 상태면 IllegalStateException 발생")
    void getCryptoPrice_should_throw_exception_when_provider_is_disabled() {
        // Given
        String symbol = "BTC";
        String baseCurrency = "KRW";

        // Provider 비활성화
        ReflectionTestUtils.setField(upbitProvider, "enabled", false);

        // When & Then
        assertThatThrownBy(() -> upbitProvider.getCryptoPrice(symbol, baseCurrency))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Upbit API가 사용 불가능합니다");
    }

    @Test
    @DisplayName("Provider 이름은 'Upbit'이어야 함")
    void getProviderName_should_return_upbit() {
        // When
        String providerName = upbitProvider.getProviderName();

        // Then
        assertThat(providerName).isEqualTo("Upbit");
    }

    @Test
    @DisplayName("Provider가 활성화 상태면 isAvailable()은 true 반환")
    void isAvailable_should_return_true_when_enabled() {
        // Given
        ReflectionTestUtils.setField(upbitProvider, "enabled", true);

        // When
        boolean available = upbitProvider.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Provider가 비활성화 상태면 isAvailable()은 false 반환")
    void isAvailable_should_return_false_when_disabled() {
        // Given
        ReflectionTestUtils.setField(upbitProvider, "enabled", false);

        // When
        boolean available = upbitProvider.isAvailable();

        // Then
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("검색 기능 지원 여부는 true")
    void supportsSearch_should_return_true() {
        assertThat(upbitProvider.supportsSearch()).isTrue();
    }

    @Test
    @DisplayName("트렌딩 기능 지원 여부는 true")
    void supportsTrending_should_return_true() {
        assertThat(upbitProvider.supportsTrending()).isTrue();
    }

    @Test
    @DisplayName("과거 데이터 지원 여부는 true")
    void supportsHistoricalData_should_return_true() {
        assertThat(upbitProvider.supportsHistoricalData()).isTrue();
    }

    // ==================== 💪 추가 연습문제 ====================

    // TODO: 여러분이 직접 작성해보세요!
    // 1. getBatchCryptoPrices() 테스트 (여러 암호화폐 동시 조회)
    @Test
    @DisplayName("[성공] 5개 암호화폐 배치 조회 - 정상 응답 시 5개의 CryptoPriceDto 반환")
    void getBatchCryptoPrices_should_return_5_prices_when_api_returns_valid_data() {
        // ========================================
        // Step 1: Given - 조회할 암호화폐 목록 준비
        // ========================================
        List<String> symbols = List.of("BTC", "ETH", "TRX", "ETC", "DOGE");
        String baseCurrency = "KRW";

        // ========================================
        // Step 2: Given - Mock 응답 데이터 준비 (Helper 메서드 활용!)
        // ========================================
        ResponseEntity<List> mockResponse = ResponseEntity.ok(List.of(
                createDummyTickerData("BTC", 50000000.0),
                createDummyTickerData("ETH", 3000000.0),
                createDummyTickerData("TRX", 150.0),
                createDummyTickerData("ETC", 25000.0),
                createDummyTickerData("DOGE", 200.0)
        ));

        // ========================================
        // Step 3: When - RestTemplate Mock 설정
        // ========================================
        // Upbit API는 배치 조회 시 이런 형식: ?markets=KRW-BTC,KRW-ETH,KRW-TRX,KRW-ETC,KRW-DOGE
        String expectedUrl = BASE_URL + "/ticker?markets=KRW-BTC,KRW-ETH,KRW-TRX,KRW-ETC,KRW-DOGE";
        when(restTemplate.getForEntity(eq(expectedUrl), eq(List.class)))
                .thenReturn(mockResponse);

        // ========================================
        // Step 4: 실행 - 실제 메서드 호출
        // ========================================
        List<CryptoPriceDto> results = upbitProvider.getBatchCryptoPrices(symbols, baseCurrency);

        // ========================================
        // Step 5: Then - 검증 (핵심만!)
        // ========================================
        assertThat(results).isNotNull();
        assertThat(results).hasSize(5); // 5개의 결과가 반환되어야 함

        // 각 심볼이 정확히 파싱되었는지 확인
        assertThat(results)
                .extracting(CryptoPriceDto::getSymbol)
                .containsExactlyInAnyOrder("BTC", "ETH", "TRX", "ETC", "DOGE");

        // 가격이 정상적으로 파싱되었는지 검증 (대표로 BTC만)
        CryptoPriceDto btcResult = results.stream()
                .filter(dto -> dto.getSymbol().equals("BTC"))
                .findFirst()
                .orElseThrow();
        assertThat(btcResult.getPrice()).isEqualTo(50000000.0);
        assertThat(btcResult.getMarketCode()).isEqualTo("KRW-BTC");
    }
    // 2. 잘못된 데이터 파싱 테스트 (price가 null인 경우)
    // 3. 대소문자 변환 테스트 (btc → BTC)

    // ==================== 🛠️ Helper Methods ====================

    /**
     * Upbit API Ticker 응답 Mock 데이터 생성
     *
     * @param symbol 암호화폐 심볼 (예: "BTC", "ETH")
     * @param price  현재가
     * @return Upbit Ticker JSON 구조의 Map
     */
    private Map<String, Object> createDummyTickerData(String symbol, double price) {
        Map<String, Object> data = new HashMap<>();
        data.put("market", "KRW-" + symbol);
        data.put("trade_price", price);
        data.put("opening_price", price * 0.98);  // 시가 (현재가의 98%)
        data.put("high_price", price * 1.02);     // 고가 (현재가의 102%)
        data.put("low_price", price * 0.97);      // 저가 (현재가의 97%)
        data.put("signed_change_price", price * 0.02);  // 변화액
        data.put("signed_change_rate", 0.02);     // 변화율 (2%)
        data.put("acc_trade_volume_24h", 1000.0); // 거래량
        data.put("acc_trade_price_24h", price * 1000);  // 거래대금
        return data;
    }
}
