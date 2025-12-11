package com.pjsent.sentinel.crypto.service;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.dto.CryptoSearchResultDto;
import com.pjsent.sentinel.crypto.service.factory.CryptoDataProviderFactory;
import com.pjsent.sentinel.crypto.service.provider.CryptoDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CryptoDataService 단위 테스트
 *
 * TDD Cycle:
 * [RED] 테스트 먼저 작성 → Fallback 로직 미구현 시 실패
 * [GREEN] CryptoDataService 구현으로 통과
 * [REFACTOR] Fallback 전략 개선
 *
 * 테스트 범위:
 * - getCryptoPrice() - Fallback 전략 (Upbit → Binance)
 * - getBatchCryptoPrices() - 배치 조회 및 개별 Fallback
 * - searchCrypto() - 암호화폐 검색
 * - 파라미터 검증 (null, empty)
 * - 에러 시나리오 처리
 */
@ExtendWith(MockitoExtension.class)
class CryptoDataServiceTest {

    @Mock
    private CryptoDataProviderFactory providerFactory;

    @Mock
    private CryptoDataProvider primaryProvider;  // Upbit

    @Mock
    private CryptoDataProvider fallbackProvider;  // Binance

    @InjectMocks
    private CryptoDataService cryptoDataService;

    private static final String TEST_SYMBOL = "BTC";
    private static final String TEST_BASE_CURRENCY = "KRW";

    // ==================== getCryptoPrice() 테스트 ====================

    @Test
    @DisplayName("[성공] 첫 번째 Provider(Upbit)에서 가격 조회 성공")
    void getCryptoPrice_should_return_price_from_primary_provider() {
        // Given
        CryptoPriceDto mockPrice = createDummyPriceDto(TEST_SYMBOL, 50000000.0, "Upbit");

        when(providerFactory.getAllProviders()).thenReturn(List.of(primaryProvider, fallbackProvider));
        when(primaryProvider.isAvailable()).thenReturn(true);
        when(primaryProvider.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY)).thenReturn(mockPrice);

        // When
        CryptoPriceDto result = cryptoDataService.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo(TEST_SYMBOL);
        assertThat(result.getPrice()).isEqualTo(50000000.0);
        assertThat(result.getProvider()).isEqualTo("Upbit");

        // Fallback Provider는 호출되지 않아야 함
        verify(primaryProvider).getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);
        verify(fallbackProvider, never()).getCryptoPrice(anyString(), anyString());
    }

    @Test
    @DisplayName("[Fallback] 첫 번째 Provider 실패 시 두 번째 Provider로 Fallback")
    void getCryptoPrice_should_fallback_to_second_provider_when_first_fails() {
        // Given
        CryptoPriceDto mockPrice = createDummyPriceDto(TEST_SYMBOL, 30000.0, "Binance");

        when(providerFactory.getAllProviders()).thenReturn(List.of(primaryProvider, fallbackProvider));
        when(primaryProvider.isAvailable()).thenReturn(true);
        when(fallbackProvider.isAvailable()).thenReturn(true);

        // Primary Provider 실패
        when(primaryProvider.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY))
                .thenThrow(new RuntimeException("Upbit API 오류"));

        // Fallback Provider 성공
        when(fallbackProvider.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY)).thenReturn(mockPrice);

        // When
        CryptoPriceDto result = cryptoDataService.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProvider()).isEqualTo("Binance");  // Fallback Provider 사용 확인
        assertThat(result.getPrice()).isEqualTo(30000.0);

        // 두 Provider 모두 호출되었는지 확인
        verify(primaryProvider).getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);
        verify(fallbackProvider).getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);
    }

    @Test
    @DisplayName("[실패] 모든 Provider 실패 시 RuntimeException 발생")
    void getCryptoPrice_should_throw_exception_when_all_providers_fail() {
        // Given
        when(providerFactory.getAllProviders()).thenReturn(List.of(primaryProvider, fallbackProvider));
        when(primaryProvider.isAvailable()).thenReturn(true);
        when(fallbackProvider.isAvailable()).thenReturn(true);

        // 모든 Provider 실패
        when(primaryProvider.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY))
                .thenThrow(new RuntimeException("Upbit 실패"));
        when(fallbackProvider.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY))
                .thenThrow(new RuntimeException("Binance 실패"));

        // When & Then
        assertThatThrownBy(() -> cryptoDataService.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("암호화폐 가격 조회 실패");

        // 두 Provider 모두 시도했는지 확인
        verify(primaryProvider).getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);
        verify(fallbackProvider).getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);
    }

    @Test
    @DisplayName("[실패] 심볼이 null이면 IllegalArgumentException 발생")
    void getCryptoPrice_should_throw_exception_when_symbol_is_null() {
        // When & Then
        assertThatThrownBy(() -> cryptoDataService.getCryptoPrice(null, TEST_BASE_CURRENCY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("심볼은 필수입니다");
    }

    @Test
    @DisplayName("[실패] 심볼이 빈 문자열이면 IllegalArgumentException 발생")
    void getCryptoPrice_should_throw_exception_when_symbol_is_empty() {
        // When & Then
        assertThatThrownBy(() -> cryptoDataService.getCryptoPrice("  ", TEST_BASE_CURRENCY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("심볼은 필수입니다");
    }

    @Test
    @DisplayName("[실패] 기준 통화가 null이면 IllegalArgumentException 발생")
    void getCryptoPrice_should_throw_exception_when_base_currency_is_null() {
        // When & Then
        assertThatThrownBy(() -> cryptoDataService.getCryptoPrice(TEST_SYMBOL, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("기준 통화는 필수입니다");
    }

    @Test
    @DisplayName("[Skip] 사용 불가능한 Provider는 건너뛰기")
    void getCryptoPrice_should_skip_unavailable_providers() {
        // Given
        CryptoPriceDto mockPrice = createDummyPriceDto(TEST_SYMBOL, 30000.0, "Binance");

        when(providerFactory.getAllProviders()).thenReturn(List.of(primaryProvider, fallbackProvider));
        when(primaryProvider.isAvailable()).thenReturn(false);  // Primary 사용 불가
        when(fallbackProvider.isAvailable()).thenReturn(true);
        when(fallbackProvider.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY)).thenReturn(mockPrice);

        // When
        CryptoPriceDto result = cryptoDataService.getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);

        // Then
        assertThat(result.getProvider()).isEqualTo("Binance");

        // Primary는 호출조차 안 됨
        verify(primaryProvider, never()).getCryptoPrice(anyString(), anyString());
        verify(fallbackProvider).getCryptoPrice(TEST_SYMBOL, TEST_BASE_CURRENCY);
    }

    // ==================== getBatchCryptoPrices() 테스트 ====================

    @Test
    @DisplayName("[성공] 배치 가격 조회 - Provider 배치 API 사용")
    void getBatchCryptoPrices_should_return_batch_prices_from_provider() {
        // Given
        List<String> symbols = List.of("BTC", "ETH", "DOGE");
        List<CryptoPriceDto> mockPrices = List.of(
                createDummyPriceDto("BTC", 50000000.0, "Upbit"),
                createDummyPriceDto("ETH", 3000000.0, "Upbit"),
                createDummyPriceDto("DOGE", 200.0, "Upbit")
        );

        when(providerFactory.getAvailableProvider()).thenReturn(primaryProvider);
        when(primaryProvider.getProviderName()).thenReturn("Upbit");
        when(primaryProvider.getBatchCryptoPrices(symbols, TEST_BASE_CURRENCY)).thenReturn(mockPrices);

        // When
        List<CryptoPriceDto> results = cryptoDataService.getBatchCryptoPrices(symbols, TEST_BASE_CURRENCY);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(CryptoPriceDto::getSymbol)
                .containsExactly("BTC", "ETH", "DOGE");

        verify(primaryProvider).getBatchCryptoPrices(symbols, TEST_BASE_CURRENCY);
    }

    @Test
    @DisplayName("[Fallback] 배치 API 실패 시 개별 조회로 Fallback")
    void getBatchCryptoPrices_should_fallback_to_individual_calls_when_batch_fails() {
        // Given
        List<String> symbols = List.of("BTC", "ETH");

        when(providerFactory.getAvailableProvider()).thenReturn(primaryProvider);
        when(primaryProvider.getBatchCryptoPrices(symbols, TEST_BASE_CURRENCY))
                .thenThrow(new RuntimeException("Batch API 오류"));

        // 개별 조회는 성공
        when(providerFactory.getAllProviders()).thenReturn(List.of(primaryProvider));
        when(primaryProvider.isAvailable()).thenReturn(true);
        when(primaryProvider.getCryptoPrice("BTC", TEST_BASE_CURRENCY))
                .thenReturn(createDummyPriceDto("BTC", 50000000.0, "Upbit"));
        when(primaryProvider.getCryptoPrice("ETH", TEST_BASE_CURRENCY))
                .thenReturn(createDummyPriceDto("ETH", 3000000.0, "Upbit"));

        // When
        List<CryptoPriceDto> results = cryptoDataService.getBatchCryptoPrices(symbols, TEST_BASE_CURRENCY);

        // Then
        assertThat(results).hasSize(2);

        // 배치 시도 후 개별 조회로 전환
        verify(primaryProvider).getBatchCryptoPrices(symbols, TEST_BASE_CURRENCY);
        verify(primaryProvider).getCryptoPrice("BTC", TEST_BASE_CURRENCY);
        verify(primaryProvider).getCryptoPrice("ETH", TEST_BASE_CURRENCY);
    }

    // ==================== searchCrypto() 테스트 ====================

    @Test
    @DisplayName("[성공] 암호화폐 검색")
    void searchCrypto_should_return_search_results() {
        // Given
        String query = "bitcoin";
        List<CryptoSearchResultDto> mockResults = List.of(
                createDummySearchResult("BTC", "Bitcoin"),
                createDummySearchResult("BCH", "Bitcoin Cash")
        );

        when(providerFactory.getSearchProvider()).thenReturn(primaryProvider);
        when(primaryProvider.searchCrypto(query)).thenReturn(mockResults);

        // When
        List<CryptoSearchResultDto> results = cryptoDataService.searchCrypto(query);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(CryptoSearchResultDto::getSymbol)
                .containsExactly("BTC", "BCH");

        verify(primaryProvider).searchCrypto(query);
    }

    @Test
    @DisplayName("[실패] 검색어가 null이면 IllegalArgumentException 발생")
    void searchCrypto_should_throw_exception_when_query_is_null() {
        // When & Then
        assertThatThrownBy(() -> cryptoDataService.searchCrypto(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("검색어는 필수입니다");
    }

    @Test
    @DisplayName("[실패] 검색어가 빈 문자열이면 IllegalArgumentException 발생")
    void searchCrypto_should_throw_exception_when_query_is_empty() {
        // When & Then
        assertThatThrownBy(() -> cryptoDataService.searchCrypto("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("검색어는 필수입니다");
    }

    // ==================== 🛠️ Helper Methods ====================

    /**
     * CryptoPriceDto Mock 데이터 생성
     */
    private CryptoPriceDto createDummyPriceDto(String symbol, double price, String provider) {
        CryptoPriceDto dto = new CryptoPriceDto();
        dto.setSymbol(symbol);
        dto.setBaseCurrency(TEST_BASE_CURRENCY);
        dto.setMarketCode(TEST_BASE_CURRENCY + "-" + symbol);
        dto.setPrice(price);
        dto.setProvider(provider);
        return dto;
    }

    /**
     * CryptoSearchResultDto Mock 데이터 생성
     */
    private CryptoSearchResultDto createDummySearchResult(String symbol, String name) {
        CryptoSearchResultDto dto = new CryptoSearchResultDto();
        dto.setSymbol(symbol);
        dto.setName(name);
        dto.setMarketCode(TEST_BASE_CURRENCY + "-" + symbol);
        return dto;
    }
}
