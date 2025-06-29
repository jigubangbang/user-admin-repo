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
    private String name;            // 이름 
    private String tel;             // 전화번호 
    private String profileImage;    // 프로필 이미지 URL (없으면 null)
    private String provider;        // "kakao", "naver", "google"
    
    // 기존 생성자 (하위 호환성)
    public SocialUserDto(String providerId, String email, String nickname, String profileImage, String provider) {
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.name = null;
        this.tel = null;
        this.profileImage = profileImage;
        this.provider = provider;
    }
    
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
        
        return new SocialUserDto(id, email, nickname, null, null, profileImage, "kakao");
    }
    
    // 네이버 API 응답에서 SocialUserDto 생성
    public static SocialUserDto fromNaver(java.util.Map<String, Object> userResponse) {
        // 1. response 객체 추출
        Object responseObj = userResponse.get("response");
        if (!(responseObj instanceof java.util.Map<?, ?> responseMapRaw)) {
            throw new RuntimeException("네이버 response 항목이 없거나 잘못된 형식입니다.");
        }
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> response = (java.util.Map<String, Object>) responseMapRaw;
        
        // 2. 고유 ID
        String id = (String) response.get("id");
        if (id == null) {
            throw new RuntimeException("네이버 사용자 ID를 찾을 수 없습니다.");
        }
        
        // 3. 이메일, 닉네임 (필수)
        String email = (String) response.get("email");
        if (email == null) {
            throw new IllegalArgumentException("이메일 제공에 동의하지 않으면 소셜 로그인 가입이 불가능합니다.");
        }
        String nickname = (String) response.get("nickname");
        
        // 5. 이름, 전화번호, 프로필이미지 (선택)
        String name = (String) response.get("name");
        String tel = (String) response.get("mobile");
        String profileImage = (String) response.get("profile_image");
        
        return new SocialUserDto(id, email, nickname, name, tel, profileImage, "naver");
    }

    // 구글 API 응답에서 SocialUserDto 생성
    public static SocialUserDto fromGoogle(java.util.Map<String, Object> userResponse) {
        // 1. 고유 ID
        String id = (String) userResponse.get("sub");
        if (id == null) {
            throw new RuntimeException("구글 사용자 ID를 찾을 수 없습니다.");
        }

        // 2. 이메일 (필수)
        String email = (String) userResponse.get("email");
        if (email == null) {
            throw new IllegalArgumentException("이메일 제공에 동의하지 않으면 소셜 로그인 가입이 불가능합니다.");
        }

        // 3. 이름 및 닉네임
        String name = (String) userResponse.get("name");
        String nickname = name != null ? name : email.split("@")[0]; // 없으면 이메일 앞부분 사용

        // 4. 프로필 이미지, 전화번호 (선택)
        String profileImage = (String) userResponse.get("picture");
        String tel = null;

        return new SocialUserDto(id, email, nickname, name, tel, profileImage, "google");
    }
}