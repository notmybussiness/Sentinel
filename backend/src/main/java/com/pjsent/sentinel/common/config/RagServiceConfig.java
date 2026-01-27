package com.pjsent.sentinel.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 서비스 라우팅 설정
 * 
 * application.yml에서 설정 가능:
 * rag:
 * service:
 * url: http://localhost:8000
 * timeout: 30s
 * enabled: true
 */
@Configuration
@ConfigurationProperties(prefix = "rag.service")
@Data
public class RagServiceConfig {

    /**
     * RAG 서비스 URL
     * Python RAG 서비스의 베이스 URL
     */
    private String url = "http://localhost:8000";

    /**
     * 타임아웃 (초)
     * AI 응답은 오래 걸릴 수 있으므로 기본값 30초
     */
    private int timeout = 30;

    /**
     * RAG 서비스 활성화 여부
     * false면 요청 시 503 반환
     */
    private boolean enabled = true;

    /**
     * 최대 재시도 횟수
     */
    private int maxRetries = 2;

    /**
     * 헬스체크 경로
     */
    private String healthPath = "/health";
}
