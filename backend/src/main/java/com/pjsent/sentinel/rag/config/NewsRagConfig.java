package com.pjsent.sentinel.rag.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Python RAG API 설정
 */
@Getter
@Configuration
public class NewsRagConfig {

    @Value("${python-rag.enabled:true}")
    private boolean enabled;

    @Value("${python-rag.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${python-rag.timeout:30000}")
    private int timeout;

    /**
     * News Search API 엔드포인트 URL
     */
    public String getSearchUrl() {
        return baseUrl + "/api/news/search";
    }

    /**
     * RAG API 사용 가능 여부 확인
     */
    public boolean isAvailable() {
        return enabled && baseUrl != null && !baseUrl.isEmpty();
    }
}
