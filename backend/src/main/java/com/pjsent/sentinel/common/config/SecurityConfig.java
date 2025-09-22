package com.pjsent.sentinel.common.config;

import com.pjsent.sentinel.user.service.JwtService;
import com.pjsent.sentinel.user.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정
 * JWT 기반 인증 및 CORS 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 비활성화 (JWT 사용)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 요청 권한 설정
            .authorizeHttpRequests(authz -> authz
                // 메인페이지 및 정적 리소스 허용
                .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // 인증 엔드포인트는 모두 허용
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Swagger UI 허용 (개발용)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Actuator 엔드포인트 허용 (모니터링용)
                .requestMatchers("/actuator/**").permitAll()
                // H2 Console 허용 (개발용)
                .requestMatchers("/h2-console/**").permitAll()
                // 모든 요청 허용 (개발용)
                .anyRequest().permitAll()
            )
            
            // JWT 필터 추가
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",  // Next.js 개발 서버
            "http://localhost:8080",  // Spring Boot 개발 서버
            "https://yourdomain.com"  // 프로덕션 도메인
        ));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
