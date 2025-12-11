package com.pjsent.sentinel.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * WithMockJwtUser 어노테이션의 SecurityContext 팩토리
 *
 * JwtAuthenticationFilter와 동일한 방식으로 Authentication 객체를 생성합니다:
 * - Principal: userId (Long)
 * - Credentials: null
 * - Authorities: 권한 목록
 */
public class WithMockJwtUserSecurityContextFactory implements WithSecurityContextFactory<WithMockJwtUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // UserDetails 생성 (JwtAuthenticationFilter와 동일)
        UserDetails userDetails = User.builder()
                .username(annotation.email())
                .password("") // 비밀번호는 JWT 인증에서 사용하지 않음
                .authorities(Arrays.stream(annotation.authorities())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .build();

        // JwtAuthenticationFilter와 동일한 방식으로 Authentication 생성
        // Principal로 userId (Long)를 설정
        Authentication auth = new UsernamePasswordAuthenticationToken(
                annotation.userId(),  // Principal: Long 타입 userId
                null,                 // Credentials: null
                userDetails.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}
