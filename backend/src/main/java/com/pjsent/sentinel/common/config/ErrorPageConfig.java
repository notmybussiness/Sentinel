package com.pjsent.sentinel.common.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 커스텀 에러 페이지 설정
 */
@Configuration
public class ErrorPageConfig {

    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        return new CustomErrorPageRegistrar();
    }

    private static class CustomErrorPageRegistrar implements ErrorPageRegistrar {

        @Override
        public void registerErrorPages(ErrorPageRegistry registry) {
            // 404 Not Found
            ErrorPage error404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error");
            
            // 500 Internal Server Error
            ErrorPage error500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error");
            
            // 403 Forbidden
            ErrorPage error403 = new ErrorPage(HttpStatus.FORBIDDEN, "/error");
            
            // 401 Unauthorized
            ErrorPage error401 = new ErrorPage(HttpStatus.UNAUTHORIZED, "/error");
            
            // 400 Bad Request
            ErrorPage error400 = new ErrorPage(HttpStatus.BAD_REQUEST, "/error");
            
            registry.addErrorPages(error400, error401, error403, error404, error500);
        }
    }
}
