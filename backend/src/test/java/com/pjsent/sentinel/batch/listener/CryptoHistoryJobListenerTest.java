package com.pjsent.sentinel.batch.listener;

import com.pjsent.sentinel.batch.producer.BatchDlqProducer;
import com.pjsent.sentinel.common.event.BatchFailureEvent;
import com.pjsent.sentinel.common.event.FailureType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 🧢 Architect TDD: CryptoHistoryJobListener 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CryptoHistoryJobListener Unit Tests")
class CryptoHistoryJobListenerTest {

    @Mock
    private BatchDlqProducer batchDlqProducer;

    @InjectMocks
    private CryptoHistoryJobListener jobListener;

    @Nested
    @DisplayName("afterJob() 테스트")
    class AfterJobTests {

        @Test
        @DisplayName("✅ Job 성공 시 DLQ 발행하지 않음")
        void shouldNotPublishWhenJobSucceeds() {
            // given
            JobExecution jobExecution = createJobExecution(BatchStatus.COMPLETED);

            // when
            jobListener.afterJob(jobExecution);

            // then: DLQ 발행 안함
            verify(batchDlqProducer, never()).publishFailure(any());
        }

        @Test
        @DisplayName("✅ Job 실패 시 DLQ에 JOB_FAILED 이벤트 발행")
        void shouldPublishWhenJobFails() {
            // given
            JobExecution jobExecution = createJobExecution(BatchStatus.FAILED);

            // when
            jobListener.afterJob(jobExecution);

            // then
            ArgumentCaptor<BatchFailureEvent> captor = ArgumentCaptor.forClass(BatchFailureEvent.class);
            verify(batchDlqProducer).publishFailure(captor.capture());

            BatchFailureEvent event = captor.getValue();
            assertThat(event.jobName()).isEqualTo("cryptoHistoryJob");
            assertThat(event.failureType()).isEqualTo(FailureType.JOB_FAILED);
        }

        @Test
        @DisplayName("✅ Step 실패 정보에서 에러 메시지 추출")
        void shouldExtractErrorMessageFromFailedStep() {
            // given
            JobExecution jobExecution = createJobExecution(BatchStatus.FAILED);

            // Step 실패 정보 추가
            StepExecution stepExecution = new StepExecution("importStep", jobExecution);
            stepExecution.setStatus(BatchStatus.FAILED);
            stepExecution.addFailureException(new RuntimeException("Skip limit of 100 exceeded"));
            jobExecution.addStepExecutions(List.of(stepExecution));

            // when
            jobListener.afterJob(jobExecution);

            // then
            ArgumentCaptor<BatchFailureEvent> captor = ArgumentCaptor.forClass(BatchFailureEvent.class);
            verify(batchDlqProducer).publishFailure(captor.capture());

            BatchFailureEvent event = captor.getValue();
            assertThat(event.errorMessage()).isEqualTo("Skip limit of 100 exceeded");
        }
    }

    @Nested
    @DisplayName("beforeJob() 테스트")
    class BeforeJobTests {

        @Test
        @DisplayName("✅ Job 시작 시 예외 없이 로깅만 수행")
        void shouldLogOnJobStart() {
            // given
            JobExecution jobExecution = createJobExecution(BatchStatus.STARTED);

            // when & then: 예외 없이 실행
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> jobListener.beforeJob(jobExecution));
        }
    }

    private JobExecution createJobExecution(BatchStatus status) {
        JobInstance jobInstance = new JobInstance(1L, "cryptoHistoryJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        jobExecution.setStatus(status);
        jobExecution.setExitStatus(new ExitStatus(status.name()));
        return jobExecution;
    }
}
