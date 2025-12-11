package com.pjsent.sentinel.common.event;

/**
 * Batch 실패 유형을 정의하는 Enum
 * DLQ 메시지에서 실패 원인 분류에 사용
 */
public enum FailureType {
    /**
     * Skip된 아이템 (skipLimit 이내로 처리됨)
     * - Reader/Processor/Writer에서 skip된 경우
     */
    SKIPPED,

    /**
     * 재시도 한도 초과로 Skip된 아이템
     * - retryLimit을 초과한 후에도 실패하여 skip된 경우
     */
    RETRY_EXHAUSTED,

    /**
     * Job 전체 실패
     * - skipLimit 초과 또는 치명적 예외로 Job이 FAILED 상태로 종료
     */
    JOB_FAILED
}
