package com.jigubangbang.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserDto {
    private String providerId;      // 소셜 플랫폼 고유 ID (카카오: "123456789")
    private String email;           // 이메일
    private String nickname;        // 닉네임
    private String profileImage;    // 프로필 이미지 URL (없으면 null)
    private String provider;        // "kakao", "naver", "google"
    
    // 카카오 API 응답에서 SocialUserDto 생성
    public static SocialUserDto fromKakao(java.util.Map<String, Object> userResponse) {
        // 1. 고유 ID
        String id = String.valueOf(userResponse.get("id"));
        
        // 2. kakao_account 추출
        Object accountObj = userResponse.get("kakao_account");
        if (!(accountObj instanceof java.util.Map<?, ?> accountMap)) {
            throw new RuntimeException("kakao_account 항목이 없거나 잘못된 형식입니다.");
        }
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> kakaoAccount = (java.util.Map<String, Object>) accountMap;
        
        // 3. 이메일
        String email = (String) kakaoAccount.get("email");
        if (email == null) {
            throw new IllegalArgumentException("이메일 제공에 동의하지 않으면 소셜 로그인 가입이 불가능합니다.");
        }
        
        // 4. 프로필
        Object profileObj = kakaoAccount.get("profile");
        String nickname = null;
        String profileImage = null;
        if (profileObj instanceof java.util.Map<?, ?> profileMapRaw) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> profile = (java.util.Map<String, Object>) profileMapRaw;
            nickname = (String) profile.get("nickname");
            profileImage = (String) profile.get("profile_image_url");
        }
        
        return new SocialUserDto(id, email, nickname, profileImage, "kakao");
    }
}