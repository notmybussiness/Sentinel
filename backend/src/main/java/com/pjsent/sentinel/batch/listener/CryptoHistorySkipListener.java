package com.pjsent.sentinel.batch.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.batch.producer.BatchDlqProducer;
import com.pjsent.sentinel.common.event.BatchFailureEvent;
import com.pjsent.sentinel.common.event.FailureType;
import com.pjsent.sentinel.crypto.entity.CryptoPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

/**
 * CryptoHistoryJob의 Skip 이벤트를 감지하여 Kafka DLQ로 발행하는 Listener
 *
 * 🦅 QA Note:
 * - Read/Process/Write 각 단계에서 Skip 발생 시 호출됨
 * - Skip된 아이템 정보와 예외를 DLQ로 전송하여 추적 가능
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CryptoHistorySkipListener implements SkipListener<CryptoPrice, CryptoPrice> {

    private final BatchDlqProducer batchDlqProducer;
    private final ObjectMapper objectMapper;

    private static final String JOB_NAME = "cryptoHistoryJob";
    private static final String STEP_NAME = "importStep";
    private static final String ITEM_TYPE = "CryptoPrice";

    /**
     * Reader에서 Skip 발생 시 호출
     * - 데이터 파싱 오류 등
     */
    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("🔸 Skip in READ: {}", t.getMessage());

        var event = BatchFailureEvent.of(
                JOB_NAME,
                STEP_NAME,
                ITEM_TYPE,
                "N/A (read failed)",
                t,
                FailureType.SKIPPED);

        batchDlqProducer.publishFailure(event);
    }

    /**
     * Processor에서 Skip 발생 시 호출
     * - 비즈니스 로직 검증 실패 등
     */
    @Override
    public void onSkipInProcess(CryptoPrice item, Throwable t) {
        log.warn("🔸 Skip in PROCESS: symbol={}, error={}", item.getSymbol(), t.getMessage());

        var event = BatchFailureEvent.of(
                JOB_NAME,
                STEP_NAME,
                ITEM_TYPE,
                serializeItem(item),
                t,
                FailureType.SKIPPED);

        batchDlqProducer.publishFailure(event);
    }

    /**
     * Writer에서 Skip 발생 시 호출
     * - DB 제약조건 위반, 재시도 한도 초과 등
     */
    @Override
    public void onSkipInWrite(CryptoPrice item, Throwable t) {
        log.warn("🔸 Skip in WRITE: symbol={}, error={}", item.getSymbol(), t.getMessage());

        // 재시도 한도 초과로 인한 Skip인지 판단
        FailureType failureType = isRetryExhausted(t)
                ? FailureType.RETRY_EXHAUSTED
                : FailureType.SKIPPED;

        var event = BatchFailureEvent.of(
                JOB_NAME,
                STEP_NAME,
                ITEM_TYPE,
                serializeItem(item),
                t,
                failureType);

        batchDlqProducer.publishFailure(event);
    }

    /**
     * 아이템을 JSON 문자열로 직렬화
     */
    private String serializeItem(CryptoPrice item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize item: {}", e.getMessage());
            return String.format("{\"symbol\":\"%s\",\"serializationError\":true}", item.getSymbol());
        }
    }

    /**
     * 재시도 한도 초과 여부 판단
     * - RetryExhaustedException 또는 특정 패턴으로 판단
     */
    private boolean isRetryExhausted(Throwable t) {
        // Spring Batch의 재시도 한도 초과 시 발생하는 예외 패턴 확인
        return t.getClass().getSimpleName().contains("RetryExhausted")
                || (t.getMessage() != null && t.getMessage().contains("retry"));
    }
}
