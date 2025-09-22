package com.pjsent.sentinel.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        // RestTemplate 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 커넥션 타임아웃 2.5초
        // 리드 타임아웃 5초
        factory.setConnectTimeout(2500);
        factory.setReadTimeout(5000);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        return restTemplate;
    }
}