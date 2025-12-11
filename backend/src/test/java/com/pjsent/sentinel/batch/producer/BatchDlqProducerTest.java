package com.pjsent.sentinel.batch.producer;

import com.pjsent.sentinel.common.event.BatchFailureEvent;
import com.pjsent.sentinel.common.event.FailureType;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 🧢 Architect TDD: BatchDlqProducer 단위 테스트
 * 
 * RED → GREEN → REFACTOR 사이클로 작성
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BatchDlqProducer Unit Tests")
class BatchDlqProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BatchDlqProducer batchDlqProducer;

    @Nested
    @DisplayName("publishFailure() 메서드")
    class PublishFailureTests {

        @Test
        @DisplayName("✅ 정상적으로 DLQ 토픽에 메시지 발행")
        void shouldPublishToDlqTopic() {
            // given
            var event = new BatchFailureEvent(
                    "cryptoHistoryJob",
                    "importStep",
                    "CryptoPrice",
                    "{\"symbol\":\"BTC\",\"price\":50000}",
                    "IllegalArgumentException",
                    "Invalid price format",
                    FailureType.SKIPPED,
                    LocalDateTime.now());

            // Mock: 성공적인 발행
            var future = CompletableFuture.completedFuture(mockSendResult());
            when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

            // when
            batchDlqProducer.publishFailure(event);

            // then: 올바른 토픽과 키로 발행 확인
            verify(kafkaTemplate).send(
                    eq(BatchDlqProducer.DLQ_TOPIC),
                    eq("cryptoHistoryJob"), // key = jobName
                    eq(event));
        }

        @Test
        @DisplayName("✅ Job 실패 이벤트도 정상 발행")
        void shouldPublishJobFailedEvent() {
            // given
            var event = BatchFailureEvent.jobFailed("cryptoHistoryJob", "Skip limit exceeded");

            var future = CompletableFuture.completedFuture(mockSendResult());
            when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

            // when
            batchDlqProducer.publishFailure(event);

            // then
            ArgumentCaptor<BatchFailureEvent> captor = ArgumentCaptor.forClass(BatchFailureEvent.class);
            verify(kafkaTemplate).send(eq(BatchDlqProducer.DLQ_TOPIC), eq("cryptoHistoryJob"), captor.capture());

            BatchFailureEvent captured = captor.getValue();
            assertThat(captured.failureType()).isEqualTo(FailureType.JOB_FAILED);
            assertThat(captured.errorMessage()).isEqualTo("Skip limit exceeded");
        }

        @Test
        @DisplayName("✅ 발행 실패 시에도 예외 전파하지 않음 (비동기 처리)")
        void shouldHandlePublishFailureGracefully() {
            // given
            var event = new BatchFailureEvent(
                    "cryptoHistoryJob",
                    "importStep",
                    "CryptoPrice",
                    "{}",
                    "Exception",
                    "Test error",
                    FailureType.SKIPPED,
                    LocalDateTime.now());

            // Mock: 발행 실패
            var future = new CompletableFuture<SendResult<String, Object>>();
            future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
            when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(future);

            // when & then: 예외 전파 없음
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> batchDlqProducer.publishFailure(event));
        }

        private SendResult<String, Object> mockSendResult() {
            @SuppressWarnings("unchecked")
            SendResult<String, Object> result = mock(SendResult.class);
            RecordMetadata metadata = mock(RecordMetadata.class);
            when(metadata.partition()).thenReturn(0);
            when(metadata.offset()).thenReturn(1L);
            when(result.getRecordMetadata()).thenReturn(metadata);
            return result;
        }
    }
}
