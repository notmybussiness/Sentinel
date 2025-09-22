package com.pjsent.sentinel.common.config;

import com.pjsent.sentinel.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 인증 필터
 * 요청 헤더의 JWT 토큰을 검증하고 인증 정보를 설정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // JWT 토큰 추출
            jwt = authHeader.substring(7);
            
            // 토큰에서 이메일 추출
            userEmail = jwtService.getEmailFromToken(jwt);
            
            // 이메일이 있고 아직 인증되지 않은 경우
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 토큰 유효성 검증
                if (jwtService.validateToken(jwt) && !jwtService.isTokenExpired(jwt)) {
                    
                    // 사용자 정보 로드
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                    
                    // 인증 토큰 생성
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    // 요청 세부 정보 설정
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Security Context에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("JWT 인증 성공. 사용자: {}", userEmail);
                } else {
                    log.warn("유효하지 않은 JWT 토큰. 사용자: {}", userEmail);
                }
            }
            
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
            // 인증 실패 시 Security Context를 비우지 않고 다음 필터로 진행
            // (인증이 필요한 엔드포인트에서 401 응답을 반환하도록)
        }

        filterChain.doFilter(request, response);
    }
}
