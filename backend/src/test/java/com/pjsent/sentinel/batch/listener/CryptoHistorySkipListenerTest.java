package com.pjsent.sentinel.batch.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.batch.producer.BatchDlqProducer;
import com.pjsent.sentinel.common.event.BatchFailureEvent;
import com.pjsent.sentinel.common.event.FailureType;
import com.pjsent.sentinel.crypto.entity.CryptoPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * 🧢 Architect TDD: CryptoHistorySkipListener 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CryptoHistorySkipListener Unit Tests")
class CryptoHistorySkipListenerTest {

    @Mock
    private BatchDlqProducer batchDlqProducer;

    private ObjectMapper objectMapper;
    private CryptoHistorySkipListener skipListener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // LocalDateTime 지원
        skipListener = new CryptoHistorySkipListener(batchDlqProducer, objectMapper);
    }

    @Nested
    @DisplayName("onSkipInRead() 테스트")
    class OnSkipInReadTests {

        @Test
        @DisplayName("✅ Read Skip 발생 시 DLQ에 이벤트 발행")
        void shouldPublishEventOnReadSkip() {
            // given
            var exception = new IllegalArgumentException("Invalid CSV format");

            // when
            skipListener.onSkipInRead(exception);

            // then
            ArgumentCaptor<BatchFailureEvent> captor = ArgumentCaptor.forClass(BatchFailureEvent.class);
            verify(batchDlqProducer).publishFailure(captor.capture());

            BatchFailureEvent event = captor.getValue();
            assertThat(event.jobName()).isEqualTo("cryptoHistoryJob");
            assertThat(event.stepName()).isEqualTo("importStep");
            assertThat(event.itemType()).isEqualTo("CryptoPrice");
            assertThat(event.itemData()).isEqualTo("N/A (read failed)");
            assertThat(event.errorType()).isEqualTo("IllegalArgumentException");
            assertThat(event.errorMessage()).isEqualTo("Invalid CSV format");
            assertThat(event.failureType()).isEqualTo(FailureType.SKIPPED);
        }
    }

    @Nested
    @DisplayName("onSkipInProcess() 테스트")
    class OnSkipInProcessTests {

        @Test
        @DisplayName("✅ Process Skip 발생 시 아이템 정보와 함께 DLQ 발행")
        void shouldPublishEventWithItemDataOnProcessSkip() {
            // given
            var item = createTestCryptoPrice("BTC", BigDecimal.valueOf(50000));
            var exception = new IllegalArgumentException("Price cannot be negative");

            // when
            skipListener.onSkipInProcess(item, exception);

            // then
            ArgumentCaptor<BatchFailureEvent> captor = ArgumentCaptor.forClass(BatchFailureEvent.class);
            verify(batchDlqProducer).publishFailure(captor.capture());

            BatchFailureEvent event = captor.getValue();
            assertThat(event.itemType()).isEqualTo("CryptoPrice");
            assertThat(event.itemData()).contains("BTC");
            assertThat(event.failureType()).isEqualTo(FailureType.SKIPPED);
        }
    }

    @Nested
    @DisplayName("onSkipInWrite() 테스트")
    class OnSkipInWriteTests {

        @Test
        @DisplayName("✅ Write Skip 발생 시 DLQ 발행")
        void shouldPublishEventOnWriteSkip() {
            // given
            var item = createTestCryptoPrice("ETH", BigDecimal.valueOf(3000));
            var exception = new RuntimeException("DB constraint violation");

            // when
            skipListener.onSkipInWrite(item, exception);

            // then
            ArgumentCaptor<BatchFailureEvent> captor = ArgumentCaptor.forClass(BatchFailureEvent.class);
            verify(batchDlqProducer).publishFailure(captor.capture());

            BatchFailureEvent event = captor.getValue();
            assertThat(event.itemData()).contains("ETH");
            assertThat(event.errorMessage()).isEqualTo("DB constraint violation");
        }

        @Test
        @DisplayName("✅ 재시도 실패 시 RETRY_EXHAUSTED 타입으로 발행")
        void shouldMarkAsRetryExhaustedWhenRetryFailed() {
            // given
            var item = createTestCryptoPrice("XRP", BigDecimal.valueOf(1));
            var exception = new RuntimeException("retry limit exceeded");

            // when
            skipListener.onSkipInWrite(item, exception);

            // then
            ArgumentCaptor<BatchFailureEvent> captor = ArgumentCaptor.forClass(BatchFailureEvent.class);
            verify(batchDlqProducer).publishFailure(captor.capture());

            BatchFailureEvent event = captor.getValue();
            assertThat(event.failureType()).isEqualTo(FailureType.RETRY_EXHAUSTED);
        }
    }

    private CryptoPrice createTestCryptoPrice(String symbol, BigDecimal price) {
        return CryptoPrice.builder()
                .symbol(symbol)
                .currency("KRW")
                .price(price)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
