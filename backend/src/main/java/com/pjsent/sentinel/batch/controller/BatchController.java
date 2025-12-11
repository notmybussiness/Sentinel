package com.pjsent.sentinel.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job cryptoHistoryJob;

    @PostMapping("/history/import")
    public ResponseEntity<String> runHistoryImport(@RequestParam(defaultValue = "BTC") String symbol) {
        try {
            log.info("🚀 배치 작업 시작: Crypto History Import (Symbol: {})", symbol);

            jobLauncher.run(cryptoHistoryJob, new JobParametersBuilder()
                    .addString("symbol", symbol)
                    .addString("timestamp", LocalDateTime.now().toString()) // 유니크 파라미터
                    .toJobParameters());

            return ResponseEntity.ok("Batch Job Started: " + symbol);
        } catch (Exception e) {
            log.error("❌ 배치 시작 실패", e);
            return ResponseEntity.internalServerError().body("Batch Job Failed: " + e.getMessage());
        }
    }
}
