package com.pjsent.sentinel.config;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * JWT 인증을 Mock하는 커스텀 어노테이션
 *
 * 사용법:
 * @WithMockJwtUser(userId = 1L)
 * void testMethod() { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockJwtUserSecurityContextFactory.class)
public @interface WithMockJwtUser {

    /**
     * Mock 사용자 ID
     */
    long userId() default 1L;

    /**
     * Mock 사용자 이메일
     */
    String email() default "test@example.com";

    /**
     * Mock 사용자 권한
     */
    String[] authorities() default {"ROLE_USER"};
}
