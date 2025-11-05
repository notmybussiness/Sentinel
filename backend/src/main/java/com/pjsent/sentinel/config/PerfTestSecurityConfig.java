package com.pjsent.sentinel.config;

import com.pjsent.sentinel.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 성능 테스트 전용 Security 설정
 *
 * ⚠️ 주의: perf 프로파일에서만 활성화됩니다.
 * ⚠️ 운영 환경에서는 절대 사용 금지!
 *
 * 간소화된 인증:
 * - JWT 토큰 유효성만 검증 (DB 조회 생략)
 * - UserDetailsService 미사용
 * - 성능 최적화
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
@Profile("perf")  // ⚠️ 성능 테스트 프로파일에서만 활성화
public class PerfTestSecurityConfig {

    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain perfTestFilterChain(HttpSecurity http) throws Exception {
        log.warn("⚠️ [PERF TEST] 간소화된 Security 설정 활성화 - 운영 환경에서 사용 금지!");

        http
                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정 (필요 시)
                .cors(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 성능 테스트 API는 인증 없이 접근 가능
                        .requestMatchers("/api/v1/auth/perf/**").permitAll()
                        .requestMatchers("/api/v1/auth/dev-login").permitAll()
                        .requestMatchers("/api/v1/auth/kakao/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // 간소화된 JWT 필터 추가
                .addFilterBefore(
                        new PerfTestJwtFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * 성능 테스트 전용 JWT 필터
     *
     * 기존 JwtAuthenticationFilter와 차이점:
     * - DB 조회 생략
     * - UserDetailsService 미사용
     * - 토큰 검증만 수행
     */
    @Slf4j
    @RequiredArgsConstructor
    static class PerfTestJwtFilter extends OncePerRequestFilter {

        private final JwtService jwtService;

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            // Authorization 헤더 추출
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // JWT 토큰 추출
                String token = authHeader.substring(7);

                // 토큰 검증 (DB 조회 없이 서명만 검증)
                if (jwtService.validateToken(token)) {
                    // 토큰에서 사용자 정보 추출
                    String email = jwtService.extractEmail(token);
                    Long userId = jwtService.extractUserId(token);

                    // 간소화된 Authentication 객체 생성 (DB 조회 없음)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,  // principal
                                    null,   // credentials
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                    // SecurityContext에 설정
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("✅ [PERF TEST] JWT 인증 성공: userId={}, email={}", userId, email);
                }
            } catch (Exception e) {
                log.warn("⚠️ [PERF TEST] JWT 검증 실패: {}", e.getMessage());
            }

            filterChain.doFilter(request, response);
        }
    }
}
