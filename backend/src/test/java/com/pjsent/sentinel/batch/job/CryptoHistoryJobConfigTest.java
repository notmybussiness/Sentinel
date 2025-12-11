package com.pjsent.sentinel.batch.job;

import com.pjsent.sentinel.batch.listener.CryptoHistoryJobListener;
import com.pjsent.sentinel.batch.listener.CryptoHistorySkipListener;
import com.pjsent.sentinel.crypto.entity.CryptoPrice;
import com.pjsent.sentinel.crypto.repository.CryptoPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Batch 단위 테스트 (Slice Test)
 * 
 * 검증 항목:
 * 1. Chunk(1000개)가 순차적으로 Commit되는가?
 * 2. 전체 데이터가 정확히 저장되는가?
 * 3. Skip: IllegalArgumentException 발생 시 해당 아이템을 건너뛰는가?
 * 4. Retry: 일시적 실패 시 재시도가 동작하는가?
 * 5. Edge Case: 빈 데이터(count=0)도 정상 처리되는가?
 */
@SpringBatchTest
@ContextConfiguration(classes = {
        CryptoHistoryJobConfigTest.BatchTestConfig.class,
        CryptoHistoryJobConfig.class,
        CryptoHistoryJobConfigTest.FaultToleranceTestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.batch.job.enabled=false" // 여러 Job이 있어도 JobLauncherApplicationRunner 충돌 방지
})
@ActiveProfiles("test")
class CryptoHistoryJobConfigTest {

    private static final int TEST_COUNT = 2500; // 3 Chunks: 1000 + 1000 + 500

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private CryptoPriceRepository cryptoPriceRepository;

    @Autowired
    private Job cryptoHistoryJob;

    @Autowired
    private Job skipTestJob;

    @Autowired
    private Job retryTestJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(cryptoHistoryJob);
        cryptoPriceRepository.deleteAll();
    }

    @Test
    @DisplayName("✅ Chunk 커밋 검증: 1000개씩 3번 Commit되어야 한다")
    void shouldCommitInChunks() throws Exception {
        // given
        JobParameters params = new JobParametersBuilder()
                .addString("symbol", "BTC")
                .addLong("count", (long) TEST_COUNT)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

        // then: Job 성공 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // then: Step 통계 검증
        StepExecution step = jobExecution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isEqualTo(TEST_COUNT);
        assertThat(step.getWriteCount()).isEqualTo(TEST_COUNT);
        assertThat(step.getCommitCount()).isEqualTo(3); // ceil(2500/1000) = 3

        // then: DB 저장 검증
        assertThat(cryptoPriceRepository.count()).isEqualTo(TEST_COUNT);
    }

    @Test
    @DisplayName("✅ 순차성 검증: timestamp가 오름차순이어야 한다")
    void shouldMaintainOrderInDatabase() throws Exception {
        // given
        JobParameters params = new JobParametersBuilder()
                .addString("symbol", "ETH")
                .addLong("count", 100L) // 작은 수로 빠르게 확인
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        jobLauncherTestUtils.launchJob(params);

        // then: 순서 검증
        var prices = cryptoPriceRepository.findAll();
        for (int i = 1; i < prices.size(); i++) {
            assertThat(prices.get(i).getTimestamp())
                    .isAfterOrEqualTo(prices.get(i - 1).getTimestamp());
        }
    }

    @Test
    @DisplayName("✅ Edge Case: count=0일 때 Job이 정상 종료되어야 한다")
    void shouldHandleEmptyData() throws Exception {
        // given
        JobParameters params = new JobParametersBuilder()
                .addString("symbol", "EMPTY")
                .addLong("count", 0L)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = jobExecution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isZero();
        assertThat(step.getWriteCount()).isZero();
        assertThat(cryptoPriceRepository.count()).isZero();
    }

    @Test
    @DisplayName("✅ Skip 검증: IllegalArgumentException 발생 시 해당 아이템을 건너뛰고 계속 진행")
    void shouldSkipIllegalArgumentException() throws Exception {
        // given: skipTestJob 사용 (10개 중 3개에서 IllegalArgumentException 발생)
        jobLauncherTestUtils.setJob(skipTestJob);
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

        // then: Job은 성공 (skipLimit=100 이내이므로)
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // then: Skip 통계 검증
        StepExecution step = jobExecution.getStepExecutions().iterator().next();
        assertThat(step.getSkipCount()).isEqualTo(3); // 3개 스킵됨
        assertThat(step.getWriteCount()).isEqualTo(7); // 10 - 3 = 7개 성공
    }

    @Test
    @DisplayName("✅ Retry 검증: 일시적 실패 시 재시도 후 성공")
    void shouldRetryOnTransientFailure() throws Exception {
        // given: retryTestJob 사용 (처음 2번 실패 후 3번째에 성공)
        jobLauncherTestUtils.setJob(retryTestJob);
        FaultToleranceTestConfig.retryCounter.set(0); // 카운터 초기화

        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

        // then: Retry 후 성공
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // then: 재시도 횟수 검증 (최소 1회 이상 재시도)
        StepExecution step = jobExecution.getStepExecutions().iterator().next();
        assertThat(step.getRollbackCount()).isGreaterThanOrEqualTo(1);
    }

    /**
     * Batch 테스트를 위한 최소 설정
     */
    @Configuration
    @EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class
    })
    @EnableJpaRepositories(basePackages = "com.pjsent.sentinel.crypto.repository")
    @EntityScan(basePackages = "com.pjsent.sentinel.crypto.entity")
    static class BatchTestConfig {
        // Mock listeners for CryptoHistoryJobConfig dependency
        @Bean
        public CryptoHistoryJobListener cryptoHistoryJobListener() {
            return new CryptoHistoryJobListener(batchDlqProducer());
        }

        @Bean
        public CryptoHistorySkipListener cryptoHistorySkipListener() {
            return new CryptoHistorySkipListener(batchDlqProducer(), new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Bean
        public com.pjsent.sentinel.batch.producer.BatchDlqProducer batchDlqProducer() {
            // Mock producer that does nothing (no Kafka in test)
            return new com.pjsent.sentinel.batch.producer.BatchDlqProducer(null) {
                @Override
                public void publishFailure(com.pjsent.sentinel.common.event.BatchFailureEvent event) {
                    // No-op for test
                }
            };
        }
    }

    /**
     * Fault Injection을 위한 Test Configuration
     */
    @Configuration
    static class FaultToleranceTestConfig {

        static final AtomicInteger retryCounter = new AtomicInteger(0);

        /**
         * Skip 테스트용 Job: 특정 아이템에서 IllegalArgumentException 발생
         */
        @Bean
        public Job skipTestJob(JobRepository jobRepository, Step skipTestStep) {
            return new JobBuilder("skipTestJob", jobRepository)
                    .start(skipTestStep)
                    .build();
        }

        @Bean
        public Step skipTestStep(JobRepository jobRepository,
                PlatformTransactionManager transactionManager) {
            return new StepBuilder("skipTestStep", jobRepository)
                    .<Integer, CryptoPrice>chunk(10, transactionManager)
                    .reader(skipTestReader())
                    .processor(skipTestProcessor())
                    .writer(noOpWriter())
                    .faultTolerant()
                    .skipLimit(100)
                    .skip(IllegalArgumentException.class)
                    .build();
        }

        @Bean
        public ItemReader<Integer> skipTestReader() {
            List<Integer> items = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                items.add(i);
            }
            return new ListItemReader<>(items);
        }

        @Bean
        public ItemProcessor<Integer, CryptoPrice> skipTestProcessor() {
            return item -> {
                // 3, 5, 7번 아이템에서 IllegalArgumentException 발생 → Skip 대상
                if (item == 3 || item == 5 || item == 7) {
                    throw new IllegalArgumentException("Bad data at index: " + item);
                }
                return CryptoPrice.builder()
                        .symbol("SKIP_TEST")
                        .currency("KRW")
                        .price(BigDecimal.valueOf(item * 1000))
                        .timestamp(LocalDateTime.now())
                        .build();
            };
        }

        /**
         * Retry 테스트용 Job: 처음 2번 실패 후 3번째에 성공
         */
        @Bean
        public Job retryTestJob(JobRepository jobRepository, Step retryTestStep) {
            return new JobBuilder("retryTestJob", jobRepository)
                    .start(retryTestStep)
                    .build();
        }

        @Bean
        public Step retryTestStep(JobRepository jobRepository,
                PlatformTransactionManager transactionManager) {
            return new StepBuilder("retryTestStep", jobRepository)
                    .<Integer, CryptoPrice>chunk(5, transactionManager)
                    .reader(retryTestReader())
                    .processor(retryTestProcessor())
                    .writer(noOpWriter())
                    .faultTolerant()
                    .retryLimit(3)
                    .retry(RuntimeException.class)
                    .build();
        }

        @Bean
        public ItemReader<Integer> retryTestReader() {
            List<Integer> items = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                items.add(i);
            }
            return new ListItemReader<>(items);
        }

        @Bean
        public ItemProcessor<Integer, CryptoPrice> retryTestProcessor() {
            return item -> {
                // 첫 번째 아이템에서만 재시도 로직 적용
                if (item == 0) {
                    int attempts = retryCounter.incrementAndGet();
                    if (attempts < 3) {
                        throw new RuntimeException("Transient failure, attempt: " + attempts);
                    }
                }
                return CryptoPrice.builder()
                        .symbol("RETRY_TEST")
                        .currency("KRW")
                        .price(BigDecimal.valueOf(item * 1000))
                        .timestamp(LocalDateTime.now())
                        .build();
            };
        }

        @Bean
        public ItemWriter<CryptoPrice> noOpWriter() {
            return items -> {
                // No-op: 실제 DB 저장 없이 통과 (Skip/Retry 동작만 검증)
            };
        }
    }
}
