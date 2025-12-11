package com.pjsent.sentinel.batch.job;

import com.pjsent.sentinel.batch.listener.CryptoHistoryJobListener;
import com.pjsent.sentinel.batch.listener.CryptoHistorySkipListener;
import com.pjsent.sentinel.crypto.entity.CryptoPrice;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CryptoHistoryJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final CryptoHistoryJobListener cryptoHistoryJobListener;
    private final CryptoHistorySkipListener cryptoHistorySkipListener;

    @Bean
    public Job cryptoHistoryJob(JobRepository jobRepository, Step importStep) {
        return new JobBuilder("cryptoHistoryJob", jobRepository)
                .listener(cryptoHistoryJobListener) // Job 레벨 리스너: 전체 실패 시 DLQ 발행
                .start(importStep)
                .build();
    }

    @Bean
    public Step importStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("importStep", jobRepository)
                .<CryptoPrice, CryptoPrice>chunk(1000, transactionManager) // 1. Transaction 단위 (1000개씩 Commit)
                .reader(historyReader(null, 0))
                .writer(historyWriter())
                .faultTolerant() // 2. 내결함성 활성화
                .retryLimit(3) // 3. 실패 시 3회 재시도 (네트워크 불안정 대비)
                .retry(Exception.class)
                .skipLimit(100) // 4. 데이터 포맷 오류 등은 100개까지 무시하고 진행
                .skip(IllegalArgumentException.class)
                .listener(cryptoHistorySkipListener) // Skip 리스너: Skip된 아이템 DLQ 발행
                .build();
    }

    /**
     * ItemReader: JobParameter로 개수 조절 가능 (테스트 시 적은 수로 설정)
     * 기본값: 3650 (10년치), 테스트: 2500 (3 Chunks)
     */
    @Bean
    @StepScope
    public ItemReader<CryptoPrice> historyReader(
            @Value("#{jobParameters['symbol']}") String symbol,
            @Value("#{jobParameters['count'] ?: 3650}") int count) {

        String targetSymbol = (symbol == null) ? "BTC" : symbol;
        List<CryptoPrice> prices = new ArrayList<>();
        LocalDateTime date = LocalDateTime.now().minusDays(count);
        Random random = new Random(42); // Seed for reproducibility
        double currentPrice = 1000.0;

        for (int i = 0; i < count; i++) {
            currentPrice = currentPrice * (1 + (random.nextGaussian() * 0.02));
            if (currentPrice < 0.1)
                currentPrice = 0.1;

            prices.add(CryptoPrice.builder()
                    .symbol(targetSymbol)
                    .currency("KRW")
                    .price(BigDecimal.valueOf(currentPrice))
                    .timestamp(date.plusDays(i))
                    .build());
        }

        log.info("📚 생성된 Mock 데이터: {} 건 (Symbol: {})", prices.size(), targetSymbol);
        return new ListItemReader<>(prices);
    }

    @Bean
    public JpaItemWriter<CryptoPrice> historyWriter() {
        return new JpaItemWriterBuilder<CryptoPrice>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
