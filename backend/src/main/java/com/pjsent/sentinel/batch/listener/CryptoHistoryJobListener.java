package com.pjsent.sentinel.batch.listener;

import com.pjsent.sentinel.batch.producer.BatchDlqProducer;
import com.pjsent.sentinel.common.event.BatchFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * CryptoHistoryJob의 Job 레벨 실행 이벤트를 감지하는 Listener
 *
 * 🦅 QA Note:
 * - Job 전체 실패 시 CRITICAL 이벤트를 DLQ로 발행
 * - Skip 한도 초과, 치명적 예외 등으로 Job이 FAILED 상태로 종료될 때 호출
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CryptoHistoryJobListener implements JobExecutionListener {

    private final BatchDlqProducer batchDlqProducer;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("🚀 Starting Job: {} (id={})",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();

        log.info("🏁 Job completed: {} with status={}", jobName, status);

        // Job이 FAILED 상태일 때만 DLQ로 발행
        if (status == BatchStatus.FAILED) {
            String errorMessage = extractErrorMessage(jobExecution);

            log.error("❌ Job FAILED: {} - {}", jobName, errorMessage);

            var event = BatchFailureEvent.jobFailed(jobName, errorMessage);
            batchDlqProducer.publishFailure(event);
        }
    }

    /**
     * JobExecution에서 에러 메시지 추출
     */
    private String extractErrorMessage(JobExecution jobExecution) {
        // Step 실패 정보에서 에러 메시지 추출
        return jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStatus() == BatchStatus.FAILED)
                .findFirst()
                .map(step -> {
                    if (step.getFailureExceptions().isEmpty()) {
                        return String.format("Step '%s' failed with status: %s",
                                step.getStepName(), step.getExitStatus().getExitDescription());
                    }
                    return step.getFailureExceptions().get(0).getMessage();
                })
                .orElse(jobExecution.getExitStatus().getExitDescription());
    }
}
