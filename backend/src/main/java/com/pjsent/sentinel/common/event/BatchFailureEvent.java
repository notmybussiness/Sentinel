package com.pjsent.sentinel.common.event;

import java.time.LocalDateTime;

/**
 * Batch 실패 이벤트 DTO
 * Spring Batch에서 실패한 아이템을 Kafka DLQ로 전송할 때 사용
 *
 * @param jobName      실패한 Job 이름 (e.g., "cryptoHistoryJob")
 * @param stepName     실패한 Step 이름 (e.g., "importStep")
 * @param itemType     아이템 타입 (e.g., "CryptoPrice")
 * @param itemData     실패한 아이템 데이터 (JSON 문자열)
 * @param errorType    예외 클래스명 (e.g., "IllegalArgumentException")
 * @param errorMessage 예외 메시지
 * @param failureType  실패 유형 (SKIPPED, RETRY_EXHAUSTED, JOB_FAILED)
 * @param timestamp    이벤트 발생 시각
 */
public record BatchFailureEvent(
        String jobName,
        String stepName,
        String itemType,
        String itemData,
        String errorType,
        String errorMessage,
        FailureType failureType,
        LocalDateTime timestamp) {
    /**
     * 간편 생성자: timestamp 자동 설정
     */
    public static BatchFailureEvent of(
            String jobName,
            String stepName,
            String itemType,
            String itemData,
            Throwable error,
            FailureType failureType) {
        return new BatchFailureEvent(
                jobName,
                stepName,
                itemType,
                itemData,
                error.getClass().getSimpleName(),
                error.getMessage(),
                failureType,
                LocalDateTime.now());
    }

    /**
     * Job 실패용 팩토리 메서드
     */
    public static BatchFailureEvent jobFailed(String jobName, String errorMessage) {
        return new BatchFailureEvent(
                jobName,
                null,
                null,
                null,
                "JobExecutionException",
                errorMessage,
                FailureType.JOB_FAILED,
                LocalDateTime.now());
    }
}
