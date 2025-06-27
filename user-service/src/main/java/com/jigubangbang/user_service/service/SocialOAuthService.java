package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.model.SocialUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SocialOAuthService {

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    private final WebClient webClient = WebClient.create();

    // 통합 소셜 로그인 사용자 정보 조회
    public SocialUserDto getUserInfo(String code, String provider) {
        switch (provider.toLowerCase()) {
            case "kakao":
                return getKakaoUserInfo(code);
            case "naver":
                // TODO: 네이버 구현
                throw new UnsupportedOperationException("네이버 로그인은 아직 지원하지 않습니다.");
            case "google":
                // TODO: 구글 구현
                throw new UnsupportedOperationException("구글 로그인은 아직 지원하지 않습니다.");
            default:
                throw new IllegalArgumentException("지원하지 않는 소셜 로그인 플랫폼입니다: " + provider);
        }
    }

    // 카카오 사용자 정보 조회
    private SocialUserDto getKakaoUserInfo(String code) {
        System.out.println("=== 카카오 토큰 요청 디버깅 ===");
        System.out.println("받은 코드: " + code);
        System.out.println("백엔드 Client ID: " + kakaoClientId);
        System.out.println("백엔드 Redirect URI: " + kakaoRedirectUri);
    
        // 1. Access Token 요청
        Map<String, Object> tokenResponse = Objects.requireNonNull(
            webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                    .with("client_id", kakaoClientId)
                    .with("redirect_uri", kakaoRedirectUri)
                    .with("code", code)
                )
                .retrieve()
                .bodyToMono(Map.class)
                .block(),
            "카카오 토큰 응답이 NULL입니다."
        );

        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new RuntimeException("카카오 AccessToken 발급 실패: " + tokenResponse);
        }

        // 2. 사용자 정보 요청
        Map<String, Object> userResponse = Objects.requireNonNull(
            webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block(),
            "카카오 사용자 정보 응답이 NULL입니다."
        );

        // 3. SocialUserDto로 변환
        return SocialUserDto.fromKakao(userResponse);
    }
}