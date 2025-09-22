package com.pjsent.sentinel.user.service;

import com.pjsent.sentinel.user.dto.*;
import com.pjsent.sentinel.user.entity.User;
import com.pjsent.sentinel.user.entity.UserSession;
import com.pjsent.sentinel.user.repository.UserRepository;
import com.pjsent.sentinel.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 인증 서비스
 * 사용자 인증 및 세션 관리를 담당하는 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final KakaoOAuthService kakaoOAuthService;
    private final JwtService jwtService;

    /**
     * Kakao 로그인 URL 조회
     */
    public String getKakaoLoginUrl() {
        return kakaoOAuthService.getKakaoLoginUrl();
    }

    /**
     * Kakao OAuth2 로그인 처리
     */
    @Transactional
    public LoginResponseDto loginWithKakao(String code) {
        log.info("Kakao OAuth2 로그인 처리 시작");

        try {
            // 1. 인증 코드를 액세스 토큰으로 교환
            KakaoTokenResponse tokenResponse = kakaoOAuthService.exchangeCodeForToken(code);
            
            // 2. 액세스 토큰으로 사용자 정보 조회
            KakaoUserInfo kakaoUserInfo = kakaoOAuthService.getUserInfo(tokenResponse.getAccessToken());
            
            // 3. 사용자 정보를 DB에 저장 또는 업데이트
            User user = saveOrUpdateUser(kakaoUserInfo);
            
            // 4. JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
            
            // 5. 사용자 세션 저장
            saveUserSession(user, accessToken, refreshToken, tokenResponse.getExpiresIn());
            
            // 6. 응답 DTO 생성
            UserDto userDto = convertToUserDto(user);
            
            log.info("Kakao OAuth2 로그인 성공. 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
            
            return LoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(tokenResponse.getExpiresIn())
                    .user(userDto)
                    .build();

        } catch (Exception e) {
            log.error("Kakao OAuth2 로그인 실패. 코드: {}, 오류: {}", code, e.getMessage());
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 토큰 갱신
     */
    @Transactional
    public LoginResponseDto refreshToken(RefreshTokenRequest request) {
        log.info("토큰 갱신 시작");

        try {
            // 1. 리프레시 토큰 검증
            if (!jwtService.validateToken(request.getRefreshToken())) {
                throw new RuntimeException("유효하지 않은 리프레시 토큰입니다");
            }

            if (jwtService.isTokenExpired(request.getRefreshToken())) {
                throw new RuntimeException("만료된 리프레시 토큰입니다");
            }

            // 2. 사용자 정보 추출
            Long userId = jwtService.getUserIdFromToken(request.getRefreshToken());
            String email = jwtService.getEmailFromToken(request.getRefreshToken());

            // 3. 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // 4. 새로운 JWT 토큰 생성
            String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
            String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

            // 5. 기존 세션 비활성화 및 새 세션 저장
            userSessionRepository.deactivateAllSessionsByUserId(user.getId());
            saveUserSession(user, newAccessToken, newRefreshToken, 900L); // 15분

            // 6. 응답 DTO 생성
            UserDto userDto = convertToUserDto(user);

            log.info("토큰 갱신 성공. 사용자 ID: {}", user.getId());

            return LoginResponseDto.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(900L)
                    .user(userDto)
                    .build();

        } catch (Exception e) {
            log.error("토큰 갱신 실패. 오류: {}", e.getMessage());
            throw new RuntimeException("토큰 갱신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String accessToken) {
        log.info("로그아웃 처리 시작");

        try {
            // 1. 토큰에서 사용자 ID 추출
            Long userId = jwtService.getUserIdFromToken(accessToken);

            // 2. 사용자의 모든 세션 비활성화
            userSessionRepository.deactivateAllSessionsByUserId(userId);

            log.info("로그아웃 성공. 사용자 ID: {}", userId);

        } catch (Exception e) {
            log.error("로그아웃 처리 실패. 오류: {}", e.getMessage());
            throw new RuntimeException("로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    public UserDto getCurrentUser(String accessToken) {
        log.info("현재 사용자 정보 조회 시작");

        try {
            // 1. 토큰에서 사용자 ID 추출
            Long userId = jwtService.getUserIdFromToken(accessToken);

            // 2. 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // 3. 응답 DTO 생성
            UserDto userDto = convertToUserDto(user);

            log.info("현재 사용자 정보 조회 성공. 사용자 ID: {}", user.getId());

            return userDto;

        } catch (Exception e) {
            log.error("현재 사용자 정보 조회 실패. 오류: {}", e.getMessage());
            throw new RuntimeException("사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자 정보 저장 또는 업데이트
     */
    private User saveOrUpdateUser(KakaoUserInfo kakaoUserInfo) {
        final String kakaoId = kakaoUserInfo.getId().toString();
        String email = kakaoUserInfo.getKakaoAccount() != null ? 
                kakaoUserInfo.getKakaoAccount().getEmail() : null;
        final String name = kakaoUserInfo.getKakaoAccount() != null && 
                kakaoUserInfo.getKakaoAccount().getProfile() != null ? 
                kakaoUserInfo.getKakaoAccount().getProfile().getNickname() : "Unknown";
        final String profileImageUrl = kakaoUserInfo.getKakaoAccount() != null && 
                kakaoUserInfo.getKakaoAccount().getProfile() != null ? 
                kakaoUserInfo.getKakaoAccount().getProfile().getProfileImageUrl() : null;

        // 이메일이 없는 경우 Kakao ID를 이메일로 사용
        if (email == null || email.isEmpty()) {
            email = kakaoId + "@kakao.com";
        }
        final String finalEmail = email;

        return userRepository.findByKakaoId(kakaoId)
                .map(user -> {
                    // 기존 사용자 정보 업데이트
                    user.updateUserInfo(name, profileImageUrl);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    // 새 사용자 생성
                    User newUser = User.builder()
                            .kakaoId(kakaoId)
                            .email(finalEmail)
                            .name(name)
                            .profileImageUrl(profileImageUrl)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * 사용자 세션 저장
     */
    private void saveUserSession(User user, String accessToken, String refreshToken, Long expiresIn) {
        String accessTokenHash = jwtService.generateTokenHash(accessToken);
        String refreshTokenHash = jwtService.generateTokenHash(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);

        UserSession session = UserSession.builder()
                .user(user)
                .accessTokenHash(accessTokenHash)
                .refreshTokenHash(refreshTokenHash)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(session);
        user.addSession(session);
    }

    /**
     * User 엔티티를 UserDto로 변환
     */
    private UserDto convertToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
