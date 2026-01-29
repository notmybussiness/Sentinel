package com.pjsent.sentinel.crypto.streaming;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * SSEStreamingService 단위 테스트
 * 
 * TDD Cycle: RED phase
 * Reactive Streams (Flux) 테스트를 위한 StepVerifier 사용
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SSEStreamingService 테스트")
class SSEStreamingServiceTest {

    @InjectMocks
    private SSEStreamingService sseStreamingService;

    @Mock
    private CryptoDataService cryptoDataService;

    @Test
    @DisplayName("startStreaming: 1초 간격으로 가격 정보를 방출해야 함")
    void startStreaming_ShouldEmitPricesAtInterval() {
        // Given
        List<String> symbols = List.of("BTC", "ETH");
        String baseCurrency = "USD";

        CryptoPriceDto price1 = CryptoPriceDto.builder()
                .symbol("BTC")
                .price(50000.0)
                .baseCurrency(baseCurrency)
                .build();

        CryptoPriceDto price2 = CryptoPriceDto.builder()
                .symbol("ETH")
                .price(3000.0)
                .baseCurrency(baseCurrency)
                .build();
        List<CryptoPriceDto> prices = List.of(price1, price2);

        when(cryptoDataService.getBatchCryptoPrices(anyList(), anyString()))
                .thenReturn(prices);

        // Virtual Time Scheduler 사용 (시간 가속)
        VirtualTimeScheduler.getOrSet();

        // When
        var flux = sseStreamingService.startStreaming(symbols, baseCurrency);

        // Then
        StepVerifier.withVirtualTime(() -> flux)
                .thenAwait(Duration.ofSeconds(1)) // 1초 대기
                .expectNext(price1, price2) // 첫 번째 배치
                .thenAwait(Duration.ofSeconds(1)) // 1초 대기
                .expectNext(price1, price2) // 두 번째 배치
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("startStreaming: 가격 데이터가 비어있으면 빈 Flux 방출 (스트림 유지)")
    void startStreaming_WhenEmptyPrices_ShouldReturnEmptyFlux() {
        // Given
        List<String> symbols = List.of("UNKNOWN");
        String baseCurrency = "USD";

        when(cryptoDataService.getBatchCryptoPrices(anyList(), anyString()))
                .thenReturn(Collections.emptyList());

        VirtualTimeScheduler.getOrSet();

        // When
        var flux = sseStreamingService.startStreaming(symbols, baseCurrency);

        // Then
        StepVerifier.withVirtualTime(() -> flux)
                .thenAwait(Duration.ofSeconds(1))
                // expectNext가 없어야 함 (Flux.empty()가 내부적으로 반환되어 해당 틱 스킵)
                .expectNoEvent(Duration.ofMillis(100))
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("startStreaming: 에러 발생 시 스트림 종료되지 않고 로그 남기고 빈 스트림 반환")
    void startStreaming_WhenError_ShouldContinueStreaming() {
        // Given
        List<String> symbols = List.of("BTC");
        String baseCurrency = "USD";

        // 첫 번째 호출은 에러, 두 번째는 성공
        CryptoPriceDto price = CryptoPriceDto.builder()
                .symbol("BTC")
                .price(50000.0)
                .baseCurrency(baseCurrency)
                .build();

        when(cryptoDataService.getBatchCryptoPrices(anyList(), anyString()))
                .thenThrow(new RuntimeException("API Connection Failed"))
                .thenReturn(List.of(price));

        VirtualTimeScheduler.getOrSet();

        // When
        var flux = sseStreamingService.startStreaming(symbols, baseCurrency);

        // Then
        StepVerifier.withVirtualTime(() -> flux)
                .thenAwait(Duration.ofSeconds(1))
                // 첫 번째 틱은 에러로 인해 empty 반환 -> 이벤트 없음
                .expectNoEvent(Duration.ofMillis(100))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext(price) // 두 번째 틱은 성공
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("기본 정보 메서드 테스트")
    void testBasicMethods() {
        assertThat(sseStreamingService.getStreamingMethod()).isEqualTo("SSE");
        assertThat(sseStreamingService.isAvailable()).isTrue();
        assertThat(sseStreamingService.getRecommendedUseCase()).contains("권장");
        assertThat(sseStreamingService.getUpdateIntervalMs()).isEqualTo(1000L);
        assertThat(sseStreamingService.getExpectedLatencyMs()).isPositive();
    }
}
