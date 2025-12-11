package com.pjsent.sentinel.batch.producer;

import com.pjsent.sentinel.common.event.BatchFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Batch 실패 이벤트를 Kafka DLQ(Dead Letter Queue)로 발행하는 Producer
 *
 * 🦁 SRE Note:
 * - DLQ 토픽: batch-failures-dlq
 * - Partition Key: jobName (같은 Job의 실패는 같은 파티션에 모임)
 * - Monitoring: Prometheus에서
 * kafka_producer_record_send_total{topic="batch-failures-dlq"} 확인
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchDlqProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public static final String DLQ_TOPIC = "batch-failures-dlq";

    /**
     * 배치 실패 이벤트를 DLQ로 발행
     *
     * @param event 발행할 실패 이벤트
     */
    public void publishFailure(BatchFailureEvent event) {
        log.warn("🚨 Batch failure detected - Publishing to DLQ: job={}, step={}, type={}, error={}",
                event.jobName(), event.stepName(), event.failureType(), event.errorMessage());

        kafkaTemplate.send(DLQ_TOPIC, event.jobName(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("❌ Failed to publish to DLQ: {}", ex.getMessage(), ex);
                    } else {
                        log.info("✅ Published to DLQ successfully: partition={}, offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
