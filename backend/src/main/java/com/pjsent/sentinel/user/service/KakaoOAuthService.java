package com.pjsent.sentinel.user.service;

import com.pjsent.sentinel.user.dto.KakaoTokenResponse;
import com.pjsent.sentinel.user.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Kakao OAuth2 서비스
 * Kakao OAuth2 인증 및 사용자 정보 조회를 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthService {

    private final RestTemplate restTemplate;

    @Value("${kakao.oauth.client-id}")
    private String clientId;

    @Value("${kakao.oauth.client-secret}")
    private String clientSecret;

    @Value("${kakao.oauth.redirect-uri}")
    private String redirectUri;

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    /**
     * Kakao 로그인 URL 생성
     */
    public String getKakaoLoginUrl() {
        return KAKAO_AUTH_URL + "?" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "response_type=code&" +
                "scope=profile_nickname account_email profile_image";
    }

    /**
     * 인증 코드를 액세스 토큰으로 교환
     */
    public KakaoTokenResponse exchangeCodeForToken(String code) {
        log.info("Kakao 토큰 교환 시작. 코드: {}", code);

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("code", code);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            KakaoTokenResponse response = restTemplate.postForObject(
                    KAKAO_TOKEN_URL, request, KakaoTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new RuntimeException("Kakao 토큰 교환 실패");
            }

            log.info("Kakao 토큰 교환 성공. 만료시간: {}초", response.getExpiresIn());
            return response;

        } catch (Exception e) {
            log.error("Kakao 토큰 교환 실패. 코드: {}, 오류: {}", code, e.getMessage());
            throw new RuntimeException("Kakao 토큰 교환 실패: " + e.getMessage());
        }
    }

    /**
     * 액세스 토큰으로 사용자 정보 조회
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        log.info("Kakao 사용자 정보 조회 시작");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            KakaoUserInfo response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL, HttpMethod.GET, request, KakaoUserInfo.class).getBody();

            if (response == null || response.getId() == null) {
                throw new RuntimeException("Kakao 사용자 정보 조회 실패");
            }

            log.info("Kakao 사용자 정보 조회 성공. 사용자 ID: {}, 이메일: {}", 
                    response.getId(), 
                    response.getKakaoAccount() != null ? response.getKakaoAccount().getEmail() : "N/A");

            return response;

        } catch (Exception e) {
            log.error("Kakao 사용자 정보 조회 실패. 액세스 토큰: {}, 오류: {}", accessToken, e.getMessage());
            throw new RuntimeException("Kakao 사용자 정보 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 리프레시 토큰으로 액세스 토큰 갱신
     */
    public KakaoTokenResponse refreshAccessToken(String refreshToken) {
        log.info("Kakao 액세스 토큰 갱신 시작");

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            KakaoTokenResponse response = restTemplate.postForObject(
                    KAKAO_TOKEN_URL, request, KakaoTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new RuntimeException("Kakao 액세스 토큰 갱신 실패");
            }

            log.info("Kakao 액세스 토큰 갱신 성공. 만료시간: {}초", response.getExpiresIn());
            return response;

        } catch (Exception e) {
            log.error("Kakao 액세스 토큰 갱신 실패. 리프레시 토큰: {}, 오류: {}", refreshToken, e.getMessage());
            throw new RuntimeException("Kakao 액세스 토큰 갱신 실패: " + e.getMessage());
        }
    }
}
