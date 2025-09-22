package com.pjsent.sentinel.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 서비스
 * JWT 토큰 생성, 검증, 파싱을 담당
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:900000}") // 15분
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7일
    private Long refreshTokenExpiration;

    /**
     * 액세스 토큰 생성
     */
    public String generateAccessToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "access");
        
        return createToken(claims, email, accessTokenExpiration);
    }

    /**
     * 리프레시 토큰 생성
     */
    public String generateRefreshToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "refresh");
        
        return createToken(claims, email, refreshTokenExpiration);
    }

    /**
     * 토큰 생성
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 토큰에서 클레임 추출
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("토큰 만료 확인 실패: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 토큰 타입 확인 (access 또는 refresh)
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 서명 키 생성
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 토큰 해시 생성 (DB 저장용)
     */
    public String generateTokenHash(String token) {
        return String.valueOf(token.hashCode());
    }
}
