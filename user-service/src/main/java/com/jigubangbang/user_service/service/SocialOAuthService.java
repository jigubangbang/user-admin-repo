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

    // 카카오 설정
    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    // 네이버 설정
    @Value("${oauth.naver.client-id}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret}")
    private String naverClientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    private final WebClient webClient = WebClient.create();

    // 통합 사용자 정보 조회
    public SocialUserDto getUserInfo(String code, String provider) {
        switch (provider.toLowerCase()) {
            case "kakao":
                return getKakaoUserInfo(code);
            case "naver":
                return getNaverUserInfo(code); 
            case "google":
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

        Map<String, Object> userResponse = Objects.requireNonNull(
            webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block(),
            "카카오 사용자 정보 응답이 NULL입니다."
        );

        return SocialUserDto.fromKakao(userResponse);
    }

    // 네이버 사용자 정보 조회
    private SocialUserDto getNaverUserInfo(String code) {
        Map<String, Object> tokenResponse = Objects.requireNonNull(
            webClient.post()
                .uri("https://nid.naver.com/oauth2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                    .with("client_id", naverClientId)
                    .with("client_secret", naverClientSecret)
                    .with("redirect_uri", naverRedirectUri)
                    .with("code", code)
                    .with("state", "randomState") 
                )
                .retrieve()
                .bodyToMono(Map.class)
                .block(),
            "네이버 토큰 응답이 NULL입니다."
        );

        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new RuntimeException("네이버 AccessToken 발급 실패: " + tokenResponse);
        }

        Map<String, Object> userResponse = Objects.requireNonNull(
            webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block(),
            "네이버 사용자 정보 응답이 NULL입니다."
        );

        return SocialUserDto.fromNaver(userResponse);
    }
}
