package com.pjsent.sentinel.user.controller;

import com.pjsent.sentinel.user.entity.User;
import com.pjsent.sentinel.user.repository.UserRepository;
import com.pjsent.sentinel.user.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 성능 테스트 전용 인증 컨트롤러
 *
 * ⚠️ 주의: perf 프로파일에서만 활성화됩니다.
 * ⚠️ 운영 환경에서는 절대 사용 금지!
 *
 * 기능:
 * - 500명의 테스트 유저에 대한 JWT 토큰 일괄 발급
 * - 24시간 만료 시간 (application-perf.yml 설정)
 */
@RestController
@RequestMapping("/api/v1/auth/perf")
@RequiredArgsConstructor
@Slf4j
@Profile("perf")  // ⚠️ 성능 테스트 프로파일에서만 활성화
public class PerfTestAuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * 모든 성능 테스트 유저의 토큰 발급
     *
     * GET /api/v1/auth/perf/tokens
     *
     * 응답 형식:
     * [
     *   {
     *     "userId": 1,
     *     "email": "perftest001@sentinel.com",
     *     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     *   },
     *   ...
     * ]
     *
     * @return 500개의 토큰 목록
     */
    @GetMapping("/tokens")
    public ResponseEntity<List<Map<String, Object>>> generateAllTokens() {
        log.warn("⚠️ [PERF TEST] 대량 토큰 발급 요청 시작");

        // 성능 테스트 유저 조회
        List<User> perfUsers = userRepository.findByEmailStartingWith("perftest");

        if (perfUsers.isEmpty()) {
            log.error("❌ 성능 테스트 유저가 없습니다. SQL 스크립트를 먼저 실행하세요.");
            return ResponseEntity.badRequest().build();
        }

        // 토큰 생성
        List<Map<String, Object>> tokens = perfUsers.stream()
                .map(user -> {
                    String accessToken = jwtService.generateAccessToken(
                            user.getId(),
                            user.getEmail(),
                            user.getNickname()
                    );

                    String refreshToken = jwtService.generateRefreshToken(
                            user.getId(),
                            user.getEmail(),
                            user.getNickname()
                    );

                    Map<String, Object> tokenInfo = new HashMap<>();
                    tokenInfo.put("userId", user.getId());
                    tokenInfo.put("email", user.getEmail());
                    tokenInfo.put("nickname", user.getNickname());
                    tokenInfo.put("accessToken", accessToken);
                    tokenInfo.put("refreshToken", refreshToken);

                    return tokenInfo;
                })
                .collect(Collectors.toList());

        log.warn("✅ [PERF TEST] 토큰 발급 완료: {} 개", tokens.size());

        return ResponseEntity.ok(tokens);
    }

    /**
     * 단일 유저 토큰 발급 (인덱스 기반)
     *
     * GET /api/v1/auth/perf/token/{index}
     *
     * @param index 유저 인덱스 (1~500)
     * @return 토큰 정보
     */
    @GetMapping("/token/{index}")
    public ResponseEntity<Map<String, Object>> generateToken(@PathVariable int index) {
        if (index < 1 || index > 500) {
            log.warn("⚠️ [PERF TEST] 잘못된 유저 인덱스: {}", index);
            return ResponseEntity.badRequest().build();
        }

        String email = String.format("perftest%03d@sentinel.com", index);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + email));

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("userId", user.getId());
        tokenInfo.put("email", user.getEmail());
        tokenInfo.put("nickname", user.getNickname());
        tokenInfo.put("accessToken", accessToken);
        tokenInfo.put("refreshToken", refreshToken);

        log.info("✅ [PERF TEST] 토큰 발급: {}", email);

        return ResponseEntity.ok(tokenInfo);
    }

    /**
     * 성능 테스트 유저 통계
     *
     * GET /api/v1/auth/perf/stats
     *
     * @return 유저 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<User> perfUsers = userRepository.findByEmailStartingWith("perftest");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", perfUsers.size());
        stats.put("expectedUsers", 500);
        stats.put("status", perfUsers.size() == 500 ? "READY" : "INCOMPLETE");
        stats.put("message", perfUsers.size() == 500
                ? "성능 테스트 준비 완료"
                : "SQL 스크립트를 실행해서 유저를 생성하세요.");

        return ResponseEntity.ok(stats);
    }
}
