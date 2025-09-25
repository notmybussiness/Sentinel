package com.pjsent.sentinel.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Yahoo Finance API 연동을 위한 설정
 */
@Configuration
public class YahooFinanceConfig {

    /**
     * JSON 파싱을 위한 ObjectMapper Bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 알려지지 않은 속성이 있어도 실패하지 않도록 설정
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Java 8 날짜/시간 API 지원 추가
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Yahoo Finance 전용 RestTemplate Bean
     * User-Agent 및 기타 필수 헤더를 포함하여 429 에러 방지
     */
    @Bean
    @Qualifier("yahooRestTemplate")
    public RestTemplate yahooRestTemplate() {
        // 커넥션 팩토리 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10초로 증가
        factory.setReadTimeout(15000);     // 15초로 증가

        RestTemplate restTemplate = new RestTemplate(factory);

        // Yahoo Finance 전용 헤더 인터셉터 추가
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new YahooFinanceHeaderInterceptor());
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }

    /**
     * Yahoo Finance API 호출을 위한 헤더 인터셉터
     * 브라우저처럼 보이도록 필수 헤더를 추가
     */
    private static class YahooFinanceHeaderInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(
                HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws IOException {

            // Yahoo Finance가 요구하는 브라우저 헤더 추가 (429 에러 방지를 위한 완전한 브라우저 시뮬레이션)
            request.getHeaders().add("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            request.getHeaders().add("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            request.getHeaders().add("Accept-Language",
                "en-US,en;q=0.9,ko;q=0.8");
            request.getHeaders().add("Accept-Encoding",
                "gzip, deflate, br");
            request.getHeaders().add("DNT", "1");
            request.getHeaders().add("Connection", "keep-alive");
            request.getHeaders().add("Upgrade-Insecure-Requests", "1");
            request.getHeaders().add("Sec-Fetch-Dest", "document");
            request.getHeaders().add("Sec-Fetch-Mode", "navigate");
            request.getHeaders().add("Sec-Fetch-Site", "none");
            request.getHeaders().add("Sec-Fetch-User", "?1");
            request.getHeaders().add("Cache-Control", "max-age=0");

            // 실제 요청 실행
            return execution.execute(request, body);
        }
    }
}